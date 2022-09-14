package fr.hammons.sffi

import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.MemoryAccess
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.ValueLayout
import jdk.incubator.foreign.MemoryLayout.PathElement
import scala.quoted.staging.*
import scala.quoted.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import jdk.incubator.foreign.CLinker.*
import java.lang.constant.Constable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object Write17 extends Write.PlatformSpecific[MemorySegment, MemoryLayout]:
  extension (m: MemoryLayout)
    def typ =
      val res =
        m.attribute(TypeKind.ATTR_NAME).nn.toScala.map(_.asInstanceOf[TypeKind])
      res
  println("building")
  given Compiler = Compiler.make(getClass().getClassLoader().nn)
  println(summon[Compiler].equals(2))
  val layout = Layout(Layout17)

  override def writeByte(
      memory: MemorySegment,
      offset: Long,
      value: Byte
  ): Unit = ???

  override def writeInt(memory: MemorySegment, offset: Long, value: Int): Unit =
    MemoryAccess.setIntAtOffset(memory, offset, value)

  override def writeFloat(
      memory: MemorySegment,
      offset: Long,
      value: Float
  ): Unit = ???

  override def writeLong(
      memory: MemorySegment,
      offset: Long,
      value: Long
  ): Unit = ???

  def genStructWriter(context: MemoryLayout) =
    context match
      case g: GroupLayout =>
        run {
          val code = genForStructStaging(g)
          println(code.show)
          code
        }
      case _ => ???

  def genStructWriterStaging(context: MemoryLayout)(using
      executionContext: ExecutionContext
  ) =
    context match
      case g: GroupLayout =>
        run(
          genForStructStaging(g))
      case _ => ???

  // todo: only exists for benchmarking compilation
  def gen(obj: Object) =
    obj match
      case m: MemoryLayout =>
        genStructWriter(m)
        ()
      case _ => ???

  private def genForStruct(
      layout: GroupLayout
  ): (MemorySegment, Long, Product) => Unit =
    val paths = layout
      .memberLayouts()
      .nn
      .asScala
      .map(_.name().nn.get.nn)
      .map(PathElement.groupElement(_).nn)

    val offsets = paths.map(layout.byteOffset(_)).toList
    val writers = paths.map(layout.select(_).nn).zip(offsets).zipWithIndex.map {
      case ((v: ValueLayout, offset), idx) if v.typ.exists(_ == TypeKind.INT) =>
        (p: Product, baseOffset: Long) =>
          writeInt(
            _,
            baseOffset + offset,
            p.productElement(idx).asInstanceOf[Int]
          )
      case ((g: GroupLayout, offset), idx) if g.isStruct() =>
        (p: Product, baseOffset: Long) =>
          genForStruct(g)(
            _,
            baseOffset + offset,
            p.productElement(idx).asInstanceOf[Product]
          )
    }

    (memory: MemorySegment, offset: Long, product: Product) =>
      writers.foreach(_(product, offset)(memory))

  private def genForStructStaging(
      layout: GroupLayout
  )(using Quotes): Expr[(MemorySegment, Long, Product) => Unit] =
    val paths = layout
      .memberLayouts()
      .nn
      .asScala
      .map(_.name().nn.get().nn)
      .map(PathElement.groupElement(_).nn)

    case class LayoutInformation(
        layout: MemoryLayout,
        offset: Long,
        index: Int,
        typ: TypeKind
    )
    val offsets = paths.map(layout.byteOffset(_)).toList
    val writers = paths
      .map(layout.select(_).nn)
      .zip(offsets)
      .zipWithIndex
      .map { case ((layout, offset), idx) =>
        LayoutInformation(layout, offset, idx, layout.typ.getOrElse(TypeKind.LONG_LONG))
      }
      .map {
        case LayoutInformation(_, offset, idx, TypeKind.INT) =>
          '{ (a: MemorySegment, baseOffset: Long, p: Product) =>
            writeInt(
              a,
              ${ Expr(offset) } + baseOffset,
              p.productElement(${ Expr(idx) }).asInstanceOf[Int]
            )
          }
        // case C_FLOAT =>
        //   '{ (a: MemorySegment, b: Long, c: Any) =>
        //     writeFloat(a, b, c.asInstanceOf[Float])
        //   }
        // case C_LONG =>
        //   '{ (a: MemorySegment, b: Long, c: Any) =>
        //     writeLong(a, b, c.asInstanceOf[Long])
        //   }
        // case C_CHAR =>
        //   '{ (a: MemorySegment, b: Long, c: Any) =>
        //     writeByte(a, b, c.asInstanceOf[Byte])
        //   }
        case LayoutInformation(layout: GroupLayout, offset, idx, _)
            if layout.isStruct() =>
          '{ (a: MemorySegment, b: Long, c: Product) =>
            ${
              Expr.betaReduce('{
                ${ genForStructStaging(layout) }(
                  a,
                  ${ Expr(offset) } + b,
                  c.productElement(${ Expr(idx) }).asInstanceOf[Product]
                )
              })
            }
          }
        case l =>
          println("huh?")
          ???
        //   '{ (a: MemorySegment, b: Long, c: Any) =>
        //     writeStruct(a, b, ?, c.asInstanceOf[Product])
        //   }
      }
      .toList

    val writer = '{ (memory: MemorySegment, offset: Long, product: Product) =>
      ${
        Expr.block(
          writers.map(w =>
            Expr.betaReduce('{ $w.apply(memory, offset, product) })
          ),
          '{ () }
        )
      }
    }
    writer

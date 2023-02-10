package fr.hammons.slinc

import scala.quoted.*
import scala.deriving.Mirror
import scala.compiletime.summonInline
import scala.util.chaining.*
import fr.hammons.slinc.container.{ContextProof, *:::, End}
import fr.hammons.slinc.modules.DescriptorModule
import scala.annotation.nowarn

trait Send[A]:
  def to(mem: Mem, offset: Bytes, value: A): Unit

object Send:
  given [A]: Fn[Send[A], (Mem, Bytes, A), Unit] with
    def andThen(function: Send[A], andThen: Unit => Unit): Send[A] =
      (mem: Mem, offset: Bytes, value: A) =>
        andThen(function.to(mem, offset, value))

  given [A](using c: ContextProof[Send *::: End, A]): Send[A] = c.tup.head

  def staged[A <: Product](
      descriptor: StructDescriptor
  )(using DescriptorModule): JitCompiler => Send[A] =
    (jitCompiler: JitCompiler) =>
      jitCompiler(
        '{
          new Send[Product]:
            def to(mem: Mem, offset: Bytes, value: Product) =
              ${
                stagedHelper(descriptor, 'mem, 'offset, 'value)
              }
        }
      ).asInstanceOf[Send[A]]

  // todo: get rid of this once bug https://github.com/lampepfl/dotty/issues/16863 is fixed
  @nowarn("msg=unused implicit parameter")
  private def asExprOf[A](expr: Expr[Any])(using Quotes, Type[A]) =
    import quotes.reflect.*
    if expr.isExprOf[A] then expr.asExprOf[A]
    else '{ $expr.asInstanceOf[A] }

  // todo: get rid of this once bug https://github.com/lampepfl/dotty/issues/16863 is fixed
  @nowarn("msg=unused implicit parameter")
  @nowarn("msg=unused local definition")
  private def stagedHelper(
      layout: TypeDescriptor,
      mem: Expr[Mem],
      offset: Expr[Bytes],
      value: Expr[Any]
  )(using Quotes, DescriptorModule): Expr[Unit] =
    import quotes.reflect.*

    layout match
      case IntDescriptor =>
        '{ $mem.writeInt(${ asExprOf[Int](value) }, $offset) }
      case LongDescriptor =>
        '{ $mem.writeLong(${ asExprOf[Long](value) }, $offset) }
      case FloatDescriptor =>
        '{ $mem.writeFloat(${ asExprOf[Float](value) }, $offset) }
      case ShortDescriptor =>
        '{ $mem.writeShort(${ asExprOf[Short](value) }, $offset) }
      case ByteDescriptor =>
        '{ $mem.writeByte(${ asExprOf[Byte](value) }, $offset) }
      case DoubleDescriptor =>
        '{ $mem.writeDouble(${ asExprOf[Double](value) }, $offset) }
      case PtrDescriptor =>
        '{ $mem.writeAddress(${ asExprOf[Ptr[Any]](value) }.mem, $offset) }
      case structDescriptor: StructDescriptor =>
        val fields =
          if canBeUsedDirectly(structDescriptor.clazz) then
            Symbol
              .classSymbol(structDescriptor.clazz.getCanonicalName().nn)
              .caseFields
          else Nil

        val offsets = structDescriptor.offsets
        val fns = structDescriptor.members.view.zipWithIndex.map {
          case (StructMemberDescriptor(childLayout, _), index) =>
            (nv: Expr[Product]) =>
              val childField =
                if fields.nonEmpty then Select(nv.asTerm, fields(index)).asExpr
                else '{ $nv.productElement(${ Expr(index) }) }

              stagedHelper(
                childLayout,
                mem,
                '{ $offset + ${ Expr(offsets(index)) } },
                childField
              )
        }.toList

        val implementation = [A] =>
          (inputExpression: Expr[A & Product]) =>
            Expr.block(fns.map(_(inputExpression)), '{})
        if canBeUsedDirectly(structDescriptor.clazz) then
          TypeRepr.typeConstructorOf(structDescriptor.clazz).asType match
            case '[a & Product] =>
              '{
                val a: a & Product = $value.asInstanceOf[a & Product]

                ${ implementation('{ a }) }
              }
        else
          '{
            val a: Product = $value.asInstanceOf[Product]

            ${
              implementation('{ a })
            }
          }
  end stagedHelper

  inline def compileTime[A <: Product](
      childOffsets: IArray[Bytes]
  )(using m: Mirror.ProductOf[A]): Send[A] =
    (mem: Mem, structOffset: Bytes, value: Product) =>
      compileTimeHelper[m.MirroredElemTypes](
        Tuple.fromProductTyped(value.asInstanceOf[A]),
        mem,
        structOffset,
        childOffsets,
        0
      )

  private inline def compileTimeHelper[A <: Tuple](
      value: A,
      mem: Mem,
      structOffset: Bytes,
      childOffsets: IArray[Bytes],
      position: Int
  ): Unit =
    inline value match
      case refinedTuple: (h *: t) =>
        summonInline[Send[h]].to(
          mem,
          childOffsets(position) + structOffset,
          refinedTuple.head
        )
        compileTimeHelper[t](
          refinedTuple.tail,
          mem,
          structOffset,
          childOffsets,
          position + 1
        )
      case _: EmptyTuple => ()

  given Send[Int] with
    def to(mem: Mem, offset: Bytes, value: Int) =
      mem.writeInt(value, offset)

  given Send[Float] with
    def to(mem: Mem, offset: Bytes, value: Float) =
      mem.writeFloat(value, offset)

  given Send[Long] with
    def to(mem: Mem, offset: Bytes, value: Long) =
      mem.writeLong(value, offset)

  given Send[Double] with
    def to(mem: Mem, offset: Bytes, value: Double) =
      mem.writeDouble(value, offset)

  given Send[Short] with
    def to(mem: Mem, offset: Bytes, value: Short) =
      mem.writeShort(value, offset)

  given Send[Byte] with
    def to(mem: Mem, offset: Bytes, value: Byte) =
      mem.writeByte(value, offset)

  given sendInt: Send[Array[Int]] with
    def to(mem: Mem, offset: Bytes, value: Array[Int]) =
      mem.writeIntArray(value, offset)

  given sendByte: Send[Array[Byte]] with
    def to(mem: Mem, offset: Bytes, value: Array[Byte]): Unit =
      mem.writeByteArray(value, offset)

  given sendArrayA[A](using DescriptorOf[A], DescriptorModule)(using
      s: Send[A]
  ): Send[Array[A]] with
    def to(mem: Mem, offset: Bytes, value: Array[A]): Unit =
      var i = 0
      while i < value.length do
        s.to(mem, offset + (DescriptorOf[A].size * i), value(i))
        i += 1

  private val ptrSend: Send[Ptr[Any]] = new Send[Ptr[Any]]:
    def to(mem: Mem, offset: Bytes, value: Ptr[Any]): Unit =
      mem.writeAddress(value.mem, offset)

  given sendPtr[A]: Send[Ptr[A]] = ptrSend.asInstanceOf[Send[Ptr[A]]]

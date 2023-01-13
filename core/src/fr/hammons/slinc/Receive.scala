package fr.hammons.slinc

import java.lang.reflect.Modifier

import scala.annotation.targetName
import scala.quoted.*
import scala.compiletime.{erasedValue, summonInline}
import scala.util.chaining.*
import scala.deriving.Mirror
import container.{ContextProof, *:::, End}

class ReceiveI(val libraryPs: LibraryI.PlatformSpecific, layoutI: LayoutI):
  inline given fnReceive[A](using Fn[A, ?, ?]): Receive[A] =
    new Receive[A]:
      def from(mem: Mem, offset: Bytes): A =
        val descriptor = Descriptor.fromFunction[A]

        MethodHandleTools.wrappedMH[A](
          libraryPs.getDowncall(mem.asAddress, descriptor)
        )

trait Receive[A]:
  def from(mem: Mem, offset: Bytes): A

object Receive:
  given fnCompat[A]: Fn[Receive[A], (Mem, Bytes), A] with
    def andThen(fn: Receive[A], andThen: A => A): Receive[A] =
      (mem: Mem, offset: Bytes) => andThen(fn.from(mem, offset))

  given [A](using c: ContextProof[Receive *::: End, A]): Receive[A] = c.tup.head

  given Receive[Int] with
    def from(mem: Mem, offset: Bytes): Int = mem.readInt(offset)

  given Receive[Long] with
    def from(mem: Mem, offset: Bytes): Long = mem.readLong(offset)

  given Receive[Byte] with
    def from(mem: Mem, offset: Bytes): Byte = mem.readByte(offset)

  given Receive[Float] with
    def from(mem: Mem, offset: Bytes): Float = mem.readFloat(offset)

  given Receive[Double] with
    def from(mem: Mem, offset: Bytes): Double = mem.readDouble(offset)

  given Receive[Short] with
    def from(mem: Mem, offset: Bytes): Short = mem.readShort(offset)

  given [A]: Receive[Ptr[A]] with
    def from(mem: Mem, offset: Bytes): Ptr[A] =
      Ptr[A](mem.readAddress(offset), Bytes(0))

  def staged[A <: Product](
      layout: StructLayout
  ): JitCompiler => Receive[A] =
    val transforms =
      getTransforms(layout).distinctBy((className, _) => className)
    val transformsArray =
      IArray.from(transforms.map((_, transform) => transform))
    val transformIndices =
      transforms.map((className, _) => className).zipWithIndex.toMap

    (jitCompiler: JitCompiler) =>
      jitCompiler {
        val implementation = (
            fns: Expr[IArray[Tuple => Product]],
            mem: Expr[Mem],
            structOffset: Expr[Bytes]
        ) =>
          stagedHelper(layout, transformIndices, mem, structOffset, fns)
            .asExprOf[Product]
        '{ (fns: IArray[Tuple => Product]) =>
          new Receive[Product]:
            def from(mem: Mem, structOffset: Bytes) = ${
              implementation('{ fns }, '{ mem }, '{ structOffset })
            }

        }
      }(transformsArray).asInstanceOf[Receive[A]]
  end staged

  private def stagedHelper(
      layout: DataLayout,
      transformIndices: Map[String, Int],
      mem: Expr[Mem],
      structOffset: Expr[Bytes],
      transforms: Expr[IArray[Tuple => Product]]
  )(using Quotes): Expr[Any] =
    layout match
      case _: FloatLayout =>
        '{ $mem.readFloat($structOffset) }
      case _: DoubleLayout =>
        '{ $mem.readDouble($structOffset) }
      case _: IntLayout =>
        '{ $mem.readInt($structOffset) }
      case _: LongLayout =>
        '{ $mem.readLong($structOffset) }
      case _: ByteLayout =>
        '{ $mem.readByte($structOffset) }
      case _: ShortLayout =>
        '{ $mem.readShort($structOffset) }
      case _: PointerLayout =>
        '{ Ptr($mem.readAddress($structOffset), Bytes(0)) }
      case u: UnionLayout =>
        ???
      case structLayout @ StructLayout(_, _, children) =>
        val transformIndex = transformIndices(
          structLayout.clazz.getName().nn
        )
        val exprs = children.collect {
          case StructMember(childLayout, Some(_), childOffset) =>
            stagedHelper(
              childLayout,
              transformIndices,
              mem,
              '{ $structOffset + ${ Expr(childOffset) } },
              transforms
            )
        }.toList

        if canBeUsedDirectly(structLayout.clazz) then
          constructFromTarget(structLayout.clazz, exprs)
        else
          '{
            $transforms(${ Expr(transformIndex) })(${
              Expr.ofTupleFromSeq(exprs)
            })
          }
  end stagedHelper

  private def getTransforms(
      layout: DataLayout
  ): Vector[(String, Tuple => Product)] =
    layout match
      case s @ StructLayout(_, _, members) =>
        (s.clazz.getName().nn, s.transform) +: members
          .map(_.layout)
          .flatMap(getTransforms)
      case _ => Vector.empty

  private def constructFromTarget(clazz: Class[?], members: List[Expr[Any]])(
      using Quotes
  ) =
    import quotes.reflect.*
    TypeRepr.typeConstructorOf(clazz).asType match
      case '[a] =>
        Apply(
          Select(
            New(TypeTree.of[a]),
            TypeRepr.of[a].typeSymbol.primaryConstructor
          ),
          members.map(_.asTerm)
        ).asExpr

  inline def compileTime[A <: Product](
      offsets: IArray[Bytes]
  )(using m: Mirror.ProductOf[A]): Receive[A] =
    (mem, offset) =>
      m.fromProduct(
        compileTimeHelper[m.MirroredElemTypes](
          mem,
          offset,
          offsets,
          0
        )
      )

  private inline def compileTimeHelper[A <: Tuple](
      mem: Mem,
      offset: Bytes,
      offsets: IArray[Bytes],
      position: Int
  ): Tuple =
    inline erasedValue[A] match
      case _: (h *: t) =>
        summonInline[Receive[h]].from(
          mem,
          offset + offsets(position)
        ) *: compileTimeHelper[t](mem, offset, offsets, position + 1)
      case _: EmptyTuple =>
        EmptyTuple

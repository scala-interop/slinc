package fr.hammons.slinc

import java.lang.reflect.Modifier

import scala.annotation.targetName
import scala.quoted.*
import scala.compiletime.{erasedValue, summonInline}
import scala.util.chaining.*
import scala.deriving.Mirror
import container.{ContextProof, *:::, End}
import fr.hammons.slinc.modules.DescriptorModule

class ReceiveI(val libraryPs: LibraryI.PlatformSpecific):
  inline given fnReceive[A](using Fn[A, ?, ?]): Receive[A] =
    new Receive[A]:
      def from(mem: Mem, offset: Bytes): A =
        val descriptor = FunctionDescriptor.fromFunction[A]

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
      descriptor: StructDescriptor
  )(using DescriptorModule): JitCompiler => Receive[A] =
    val transforms =
      getTransforms(descriptor).distinctBy((className, _) => className)
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
          stagedHelper(descriptor, transformIndices, mem, structOffset, fns)
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
      layout: TypeDescriptor,
      transformIndices: Map[String, Int],
      mem: Expr[Mem],
      structOffset: Expr[Bytes],
      transforms: Expr[IArray[Tuple => Product]]
  )(using Quotes, DescriptorModule): Expr[Any] =
    layout match
      case FloatDescriptor =>
        '{ $mem.readFloat($structOffset) }
      case DoubleDescriptor =>
        '{ $mem.readDouble($structOffset) }
      case IntDescriptor =>
        '{ $mem.readInt($structOffset) }
      case LongDescriptor =>
        '{ $mem.readLong($structOffset) }
      case ByteDescriptor =>
        '{ $mem.readByte($structOffset) }
      case ShortDescriptor =>
        '{ $mem.readShort($structOffset) }
      case PtrDescriptor =>
        '{ Ptr($mem.readAddress($structOffset), Bytes(0)) }
      case structDescriptor: StructDescriptor =>
        val transformIndex = transformIndices(
          structDescriptor.clazz.getName().nn
        )
        val offsets = structDescriptor.offsets
        val exprs = structDescriptor.members.view.zipWithIndex.collect {
          case (StructMemberDescriptor(childLayout, name), index) =>
            stagedHelper(
              childLayout,
              transformIndices,
              mem,
              '{ $structOffset + ${ Expr(offsets(index)) } },
              transforms
            )
        }.toList

        if canBeUsedDirectly(structDescriptor.clazz) then
          constructFromTarget(structDescriptor.clazz, exprs)
        else
          '{
            $transforms(${ Expr(transformIndex) })(${
              Expr.ofTupleFromSeq(exprs)
            })
          }
  end stagedHelper

  private def getTransforms(
      descriptor: TypeDescriptor
  ): Seq[(String, Tuple => Product)] =
    descriptor match
      case s: StructDescriptor =>
        (s.clazz.getName().nn, s.transform) +: s.members
          .map(_.descriptor)
          .flatMap(getTransforms)
      case _ => Seq.empty

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

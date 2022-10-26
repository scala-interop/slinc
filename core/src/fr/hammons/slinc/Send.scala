package fr.hammons.slinc

import scala.quoted.*
import scala.annotation.targetName
import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}
import scala.util.chaining.*

trait Send[A]:
  def to(mem: Mem, offset: Bytes, value: A): Unit

object Send:
  given [A]: Fn[Send[A], (Mem, Bytes, A), Unit] with
    def andThen(function: Send[A], andThen: Unit => Unit): Send[A] =
      (mem: Mem, offset: Bytes, value: A) =>
        andThen(function.to(mem, offset, value))

    @targetName("complexAndThen")
    def andThen[ZZ](
        function: Send[A],
        andThen: Unit => ZZ
    ): FnCalc[(Mem, Bytes, A), ZZ] = (mem: Mem, offset: Bytes, value: A) =>
      andThen(function.to(mem, offset, value))

  def staged[A <: Product](layout: StructLayout): JitCompiler => Send[A] =
    (jitCompiler: JitCompiler) =>
      jitCompiler(
        '{
          new Send[Product]:
            def to(mem: Mem, offset: Bytes, value: Product) =
              ${
                stagedHelper(layout, 'mem, 'offset, 'value)
              }
        }
      ).asInstanceOf[Send[A]]

  private def asExprOf[A](expr: Expr[Any])(using Quotes, Type[A]) =
    import quotes.reflect.*
    if expr.isExprOf[A] then expr.asExprOf[A]
    else '{ $expr.asInstanceOf[A] }

  private def stagedHelper(
      layout: DataLayout,
      mem: Expr[Mem],
      offset: Expr[Bytes],
      value: Expr[Any]
  )(using Quotes): Expr[Unit] =
    import quotes.reflect.*

    layout match
      case _: IntLayout =>
        '{ $mem.writeInt(${ asExprOf[Int](value) }, $offset) }
      case _: LongLayout =>
        '{ $mem.writeLong(${ asExprOf[Long](value) }, $offset) }
      case _: FloatLayout =>
        '{ $mem.writeFloat(${ asExprOf[Float](value) }, $offset) }
      case _: ShortLayout =>
        '{ $mem.writeShort(${ asExprOf[Short](value) }, $offset) }
      case _: ByteLayout =>
        '{ $mem.writeByte(${ asExprOf[Byte](value) }, $offset) }
      case _: DoubleLayout =>
        '{ $mem.writeDouble(${ asExprOf[Double](value) }, $offset) }
      case structLayout @ StructLayout(_, _, children) =>
        val fields =
          if canBeUsedDirectly(structLayout.clazz) then
            Symbol
              .classSymbol(structLayout.clazz.getCanonicalName().nn)
              .caseFields
          else Nil

        val fns = children.zipWithIndex.map {
          case (StructMember(childLayout, _, childOffset), index) =>
            (nv: Expr[Product]) =>
              val childField =
                if fields.nonEmpty then Select(nv.asTerm, fields(index)).asExpr
                else '{ $nv.productElement(${ Expr(index) }) }

              stagedHelper(
                childLayout,
                mem,
                '{ $offset + ${ Expr(childOffset) } },
                childField
              )
        }.toList

        val implementation = [A] =>
          (inputExpression: Expr[A & Product]) =>
            Expr.block(fns.map(_(inputExpression)), '{})
        if canBeUsedDirectly(structLayout.clazz) then
          TypeRepr.typeConstructorOf(structLayout.clazz).asType match
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
    inline def to(mem: Mem, offset: Bytes, value: Int) =
      mem.writeInt(value, offset)

  given Send[Float] with
    inline def to(mem: Mem, offset: Bytes, value: Float) =
      mem.writeFloat(value, offset)

  given Send[Long] with
    inline def to(mem: Mem, offset: Bytes, value: Long) =
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

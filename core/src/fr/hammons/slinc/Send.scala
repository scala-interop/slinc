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
        }.tap(_.pipe(_.show).tap(println))
      ).asInstanceOf[Send[A]]

  private def stagedHelper(
      layout: DataLayout,
      mem: Expr[Mem],
      offset: Expr[Bytes],
      value: Expr[Any]
  )(using Quotes): Expr[Unit] =
    import quotes.reflect.*

    layout match
      case IntLayout(_, _, _) =>
        '{ $mem.writeInt($value.asInstanceOf[Int], $offset) }
      case LongLayout(_, _, _) =>
        '{ $mem.writeLong($value.asInstanceOf[Long], $offset) }
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

        if canBeUsedDirectly(structLayout.clazz) then
          TypeRepr.typeConstructorOf(structLayout.clazz).asType match
            case '[a & Product] =>
              '{
                val a: a & Product = $value.asInstanceOf[a & Product]

                ${
                  Expr.block(fns.map(fn => fn('a)), '{})
                }
              }
        else
          '{
            val a: Product = $value.asInstanceOf[Product]

            ${
              Expr.block(fns.map(_('a)), '{})
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

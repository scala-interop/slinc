package fr.hammons.sffi

import scala.quoted.*
import scala.annotation.targetName
import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}

trait Send[A]:
  def to(mem: Mem, offset: Bytes, a: A): Unit

object Send:

  given [A]: Fn[Send[A], (Mem, Bytes, A), Unit] with 
    def andThen(fn: Send[A], andThen: Unit => Unit): Send[A] = (mem: Mem, offset: Bytes, a: A) => andThen(fn.to(mem, offset, a))
    @targetName("complexAndThen")
    def andThen[ZZ](fn: Send[A], andThen: Unit => ZZ): FnCalc[(Mem, Bytes, A), ZZ] = (mem: Mem, offset: Bytes, a: A) => andThen(fn.to(mem, offset, a))
    //val eq = summon[Send[A] =:= FnCalc[(Mem,Bytes, A), Unit]]

  private def stagedHelper(
      layout: DataLayout,
      rawMem: Expr[Mem],
      offset: Expr[Bytes],
      value: Expr[Any]
  )(using Quotes): Expr[Unit] =
    layout match
      case IntLayout(_, _) =>
        '{ $rawMem.writeInt($value.asInstanceOf[Int], $offset) }
      case LongLayout(_,_) =>
        '{ $rawMem.writeLong($value.asInstanceOf[Long], $offset)}
      case StructLayout(_, _, children) =>
        //val nv = value.asExprOf[Product]
        val fns = children.zipWithIndex.map {
          case (StructMember(childLayout, _, subOffset), idx) =>
            stagedHelper(
              childLayout,
              rawMem,
              '{ $offset + ${ Expr(subOffset) } },
              '{ $value.asInstanceOf[Product].productElement(${ Expr(idx) }) }
            )
        }.toList
        Expr.block(fns, '{})

  def staged(layout: StructLayout)(using Quotes): Expr[Send[Product]] =
    '{ 
      new Send[Product]:
        def to(mem: Mem, offset: Bytes, a: Product) =     
          ${
            stagedHelper(layout, 'mem, 'offset, 'a)
          }
    }
  
  inline def compileTime[A <: Product](
      offsets: IArray[Bytes]
  )(using m: Mirror.ProductOf[A]): Send[Product] =
    (rawMem: Mem, offset: Bytes, a: Product) =>
      compileTimeHelper[m.MirroredElemTypes](
        Tuple.fromProduct(a.asInstanceOf[A]).toArray,
        rawMem,
        offset,
        offsets,
        0
      )
  private inline def compileTimeHelper[A <: Tuple](
      a: Array[Object],
      rawMem: Mem,
      offset: Bytes,
      offsets: IArray[Bytes],
      position: Int
  ): Unit =
    inline erasedValue[A] match
      case _: (h *: t) =>
        summonInline[Send[h]].to(
          rawMem,
          offsets(position) + offset,
          a(position).asInstanceOf[h]
        )
        compileTimeHelper[t](a, rawMem, offset, offsets, position + 1)
      case _: EmptyTuple => ()

  given Send[Int] with
    inline def to(mem: Mem, offset: Bytes, a: Int) = mem.writeInt(a, offset)

  given Send[Float] with
    inline def to(mem: Mem, offset: Bytes, a: Float) = mem.writeFloat(a, offset)

  given Send[Long] with
    inline def to(mem: Mem, offset: Bytes, a: Long) = mem.writeLong(a, offset)

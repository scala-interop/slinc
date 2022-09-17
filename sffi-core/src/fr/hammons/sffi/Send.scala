package fr.hammons.sffi

import scala.quoted.*
import scala.annotation.targetName

trait Send[A]:
  def to(mem: Mem, offset: Bytes, a: A): Unit

object Send:

  given [A]: Fn[Send[A], (Mem, Bytes, A), Unit] with 
    def andThen(fn: Send[A], andThen: Unit => Unit): Send[A] = (mem: Mem, offset: Bytes, a: A) => andThen(fn.to(mem, offset, a))
    @targetName("complexAndThen")
    def andThen[ZZ](fn: Send[A], andThen: Unit => ZZ): FnCalc[(Mem, Bytes, A), ZZ] = (mem: Mem, offset: Bytes, a: A) => andThen(fn.to(mem, offset, a))
    //val eq = summon[Send[A] =:= FnCalc[(Mem,Bytes, A), Unit]]

  private def sendGenHelper(
      layout: DataLayout,
      rawMem: Expr[Mem],
      offset: Expr[Bytes],
      value: Expr[Any]
  )(using Quotes): Expr[Unit] =
    layout match
      case IntLayout(_, _) =>
        '{ $rawMem.write($value.asInstanceOf[Int], $offset) }
      case StructLayout(_, _, children) =>
        //val nv = value.asExprOf[Product]
        val fns = children.zipWithIndex.map {
          case (StructMember(childLayout, _, subOffset), idx) =>
            sendGenHelper(
              childLayout,
              rawMem,
              '{ $offset + ${ Expr(subOffset) } },
              '{ $value.asInstanceOf[Product].productElement(${ Expr(idx) }) }
            )
        }.toList
        Expr.block(fns, '{})

  def sendStaged(layout: DataLayout)(using Quotes): Expr[Send[Object]] =
    '{ (mem: Mem, offset: Bytes, a: Object) =>
      ${
        sendGenHelper(layout, 'mem, 'offset, 'a)
      }
    }

given Send[Int] with
  def to(mem: Mem, offset: Bytes, a: Int) = mem.write(a, offset)

given Send[Float] with
  def to(mem: Mem, offset: Bytes, a: Float) = mem.write(a, offset)

given Send[Long] with
  def to(mem: Mem, offset: Bytes, a: Long) = mem.write(a, offset)

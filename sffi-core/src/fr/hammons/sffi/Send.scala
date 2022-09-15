package fr.hammons.sffi

import scala.quoted.*

trait Send[A]:
  def to(mem: Mem, offset: Bytes, a: A): Unit

object Send:
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
            Expr.betaReduce(sendGenHelper(
              childLayout,
              rawMem,
              '{ $offset + ${ Expr(subOffset) } },
              '{ $value.asInstanceOf[Product].productElement(${ Expr(idx) }) }
            ))
        }.toList
        Expr.block(fns, '{})

  def sendStaged(layout: DataLayout)(using Quotes): Expr[Send[Product]] =
    '{ (mem: Mem, offset: Bytes, a: Product) =>
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

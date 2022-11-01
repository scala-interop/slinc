package fr.hammons.slinc

import scala.quoted.*
import container.{ContextProof, *:::, End}

trait NativeOutCompatible[A]

object NativeOutCompatible:
  given NativeOutCompatible[Byte] with {}
  given NativeOutCompatible[Short] with {}
  given NativeOutCompatible[Int] with {}
  given NativeOutCompatible[Long] with {}
  given NativeOutCompatible[Char] with {}
  given NativeOutCompatible[Float] with {}
  given NativeOutCompatible[Double] with {}

  given [A](using
      c: ContextProof[NativeOutCompatible *::: End, A]
  ): NativeOutCompatible[A] = c.tup.head

  def handleOutput[R](
      expr: Expr[Any | Null]
  )(using Quotes, Type[R]): Expr[R] =
    Expr
      .summon[OutTransitionNeeded[R]]
      .map { fn =>
        val nExpr =
          if expr.isExprOf[Object] then expr.asExprOf[Object]
          else '{ $expr.asInstanceOf[Object] }
        '{ $fn.out($nExpr) }
      }
      .getOrElse(Type.of[R] match {
        case '[Unit] => '{ $expr; () }.asExprOf[R]
        case _ =>
          if expr.isExprOf[R] then expr.asExprOf[R]
          else '{ $expr.asInstanceOf[R] }
      })

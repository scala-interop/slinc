package fr.hammons.slinc

import scala.quoted.*

trait NativeOutCompatible[A]

object NativeOutCompatible:
  given NativeOutCompatible[Byte] with {}
  given NativeOutCompatible[Short] with {}
  given NativeOutCompatible[Int] with {}
  given NativeOutCompatible[Long] with {}
  given NativeOutCompatible[Char] with {}
  given NativeOutCompatible[Float] with {}
  given NativeOutCompatible[Double] with {} 

  def handleOutput[R](
      expr: Expr[Any | Null]
  )(using Quotes, Type[R]): Expr[R] =
    Expr
      .summon[OutTransitionNeeded[R]]
      .map(fn => '{ $fn.out($expr.asInstanceOf[Object]) })
      .getOrElse(Type.of[R] match {
        case '[Unit] => '{ $expr; () }.asExprOf[R]
        case _       => '{ $expr.asInstanceOf[R] }
      })

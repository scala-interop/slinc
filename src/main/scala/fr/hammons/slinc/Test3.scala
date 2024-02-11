package fr.hammons.slinc

import quoted.*

object Test3 {
  transparent inline def calc[T]: Int = ${
    calcImpl[T]
  }

  def calcImpl[T](using Quotes, Type[T]): Expr[Int] =
    import quotes.reflect.*
    val value = Expr
      .summon[Test2[T]]
      .map(exp => exp.asExprOf[Int].valueOrAbort)
      .getOrElse(???)

    report.errorAndAbort(value.toString())
}

package fr.hammons.sffi

import scala.quoted.*


inline def nameOf[T] = ${
  nameOfImpl[T]
}

private def nameOfImpl[T](using Quotes, Type[T]): Expr[String] = 
  import quotes.reflect.*
  Expr(TypeRepr.of[T].classSymbol.get.fullName)

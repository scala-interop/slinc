package io.gitlab.mhammons.slinc

import scala.quoted.*

inline def deriveLayout[T] = ${
  deriveLayoutImpl[T]
}

def deriveLayoutImpl[T: Type](using Quotes) =
  import quotes.reflect.*
  println("hello")
  val fields =
    TypeRepr.of[T].classSymbol.toList.flatMap(_.declarations).filter(_.isValDef)
  '{
    println(${ Expr(fields.map(_.toString).toString) })
  }

package io.gitlab.mhammons.slinc

import scala.quoted.*

import jdk.incubator.foreign.CLinker
import scala.jdk.OptionConverters.*

inline def type2String[T]: String = ${
   type2StringImpl[T]
}

def type2StringImpl[T: Type](using Quotes) =
   import quotes.reflect.*

   Expr(TypeRepr.of[T].typeSymbol.fullName)

val clookup = Function.unlift(
  ((s: String) => CLinker.systemLookup.lookup(s))
     .andThen(_.toScala)
)

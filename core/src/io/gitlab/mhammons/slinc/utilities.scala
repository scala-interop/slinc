package io.gitlab.mhammons.slinc

import scala.quoted.*

inline def type2String[T]: String = ${
   type2StringImpl[T]
}

def type2StringImpl[T: Type](using Quotes) =
   import quotes.reflect.*

   Expr(TypeRepr.of[T].typeSymbol.fullName)

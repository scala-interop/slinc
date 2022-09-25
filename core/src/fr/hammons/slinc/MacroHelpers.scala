package fr.hammons.slinc

import scala.quoted.*

object MacroHelpers:
  def widenExpr(t: Expr[?])(using Quotes) =
    import quotes.reflect.*
    t match
      case '{ $a: a } =>
        TypeRepr.of[a].widen.asType match
          case '[b] => 
            val b = a.asExprOf[b]
            '{$b: b}



  def getClassSymbol[L](using Quotes, Type[L]) =
    import quotes.reflect.*
    TypeRepr
      .of[L]
      .classSymbol
      .getOrElse(
        report.errorAndAbort(s"Cannot find information about ${Type.show[L]}")
      )

  def findOwningClass(using q: Quotes)(s: q.reflect.Symbol): q.reflect.Symbol =
    if s.isClassDef then s
    else findOwningClass(s.owner)

  def findOwningMethod(using q: Quotes)(s: q.reflect.Symbol): q.reflect.Symbol =
    if s.isDefDef then s
    else findOwningMethod(s.owner)



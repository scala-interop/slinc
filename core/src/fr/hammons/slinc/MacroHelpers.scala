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
            '{ $b: b }

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

  def getMethodSymbols(using q: Quotes)(s: q.reflect.Symbol) =
    s.declaredMethods.filter(_.name != "writeReplace")

  def getInputsAndOutputType(using
      q: Quotes
  )(methodSymbol: q.reflect.Symbol) =
    import quotes.reflect.*
    methodSymbol.tree match
      case DefDef(_, params, ret, _) =>
        params.last match
          case TermParamClause(valDefs) =>
            val inputs =
              valDefs
                .map(t => MacroHelpers.widenExpr(Ref(t.symbol).asExpr))

            inputs -> ret.tpe.asType

  def assertIsFunction[A](using Quotes, Type[A]) =
    import quotes.reflect.*
    if !TypeRepr.of[A].typeSymbol.name.startsWith("Function") then
      report.errorAndAbort(
        s"Function input required for this method, got ${Type.show[A]} instead",
        Position.ofMacroExpansion
      )

  def getInputTypesAndOutputTypes[A](using Quotes, Type[A]) =
    import quotes.reflect.*
    assertIsFunction[A]

    val args = TypeRepr.of[A].typeArgs.map(_.asType)
    args.init -> args.last

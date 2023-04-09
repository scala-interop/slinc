package fr.hammons.slinc

import scala.quoted.*
import scala.annotation.nowarn

private[slinc] object MacroHelpers:
  // todo: remove once https://github.com/lampepfl/dotty/issues/16863 is fixed
  @nowarn("msg=unused local definition")
  @nowarn("msg=unused implicit parameter")
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

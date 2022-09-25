package fr.hammons.slinc

import scala.quoted.*
import java.lang.invoke.MethodHandle

object MethodHandleTools:
  def invokeArguments(mh: Expr[MethodHandle], exprs: Expr[Any]*)(using
      Quotes
  ) =
    import quotes.reflect.*

    val mod =
      TypeRepr.of[MethodHandleFacade].classSymbol.getOrElse(???).companionModule

    val arity = exprs.size
    val methodSymbol = mod.declaredMethods
      .find(_.name.endsWith(arity.toString()))

    val call = methodSymbol.map(ms => 
      Apply(
      Select(Ident(mod.termRef), ms),
      mh.asTerm :: exprs.map(_.asTerm).toList
    ).asExprOf[Object|Null]).getOrElse(
      '{MethodHandleFacade.callVariadic($mh, ${Varargs(exprs)}*)}
    )

    val expr = call.asExprOf[Object | Null]
    report.info(expr.show)
    expr

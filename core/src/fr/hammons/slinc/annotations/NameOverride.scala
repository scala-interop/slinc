package fr.hammons.slinc.annotations

import scala.annotation.StaticAnnotation
import scala.quoted.*

import fr.hammons.slinc.types.{OS, Arch}

class NameOverride(val name: String, val platforms: (OS, Arch)*)
    extends StaticAnnotation

object NameOverride:
  inline def apply[L](methodName: String): List[NameOverride] = ${
    apply[L]('methodName)
  }
  private def apply[L](
      methodNameExpr: Expr[String]
  )(using Quotes, Type[L]): Expr[List[NameOverride]] =
    import quotes.reflect.*

    val symbol = TypeRepr
      .of[L]
      .classSymbol
      .get
      .declaredMethod(methodNameExpr.valueOrAbort)
      .head

    Expr.ofList(
      symbol.annotations
        .map(_.asExpr)
        .filter(_.isExprOf[NameOverride])
        .map(_.asExprOf[NameOverride])
    )

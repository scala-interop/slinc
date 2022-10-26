package fr.hammons.slinc

import scala.annotation.StaticAnnotation
import scala.quoted.*

class SlincImpl(version: Int) extends StaticAnnotation

object SlincImpl:
  inline def findImpls(): Map[Int, () => Slinc] = ${
    findImplsImpl
  }
  private def findImplsImpl(using q: Quotes): Expr[Map[Int, () => Slinc]] =
    import quotes.reflect.*
    val syms = Symbol
      .requiredPackage("fr.hammons.slinc")
      .declarations
      .flatMap { s =>
        s.annotations.collect {
          case Apply(
                Select(New(TypeIdent("SlincImpl")), "<init>"),
                List(Literal(IntConstant(version)))
              ) =>
            s -> version
        }.headOption
      }
      .filter((symbol, _) => symbol.declaredField("default").exists)
      .map((s, v) =>
        val versionExpr = Expr(v)
        val slincRef = Ref(s.declaredField("default")).asExprOf[Slinc]

        '{ $versionExpr -> (() => $slincRef) }
      )
      .toSeq
    val list = Expr.ofList(syms)
    '{ $list.toMap }

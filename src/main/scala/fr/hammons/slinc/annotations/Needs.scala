package fr.hammons.slinc.annotations

import scala.annotation.StaticAnnotation
import fr.hammons.slinc.fset.Dependency
import scala.quoted.*

final case class Needs(libName: String)
    extends StaticAnnotation,
      DependencyAnnotation:
  def toDependency: Dependency = Dependency.PathLibrary(libName)

object Needs:
  inline def apply[F]: List[Needs] = ${
    apply[F]()
  }

  def apply[F]()(using Quotes, Type[F]) =
    import quotes.reflect.*

    Expr.ofList(
      TypeRepr
        .of[F]
        .classSymbol
        .get
        .annotations
        .view
        .map(_.asExpr)
        .collect:
          case n @ '{ new Needs(${ _ }) } => n
        .toList
    )

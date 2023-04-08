package fr.hammons.slinc.annotations

import scala.annotation.StaticAnnotation

import quoted.*
import fr.hammons.slinc.fset.Dependency
import java.nio.file.Paths

final case class NeedsResource(val resourcePath: String)
    extends StaticAnnotation,
      DependencyAnnotation:
  def toDependency: Dependency = resourcePath match
    case path @ s"${_}.c" => Dependency.CResource(path)
    case path =>
      Dependency.LibraryResource(
        path,
        path.endsWith(".so") || path.endsWith(".dll")
      )

object NeedsResource:
  inline def apply[L]: List[NeedsResource] = ${
    apply[L]()
  }

  private def apply[L]()(using Quotes, Type[L]): Expr[List[NeedsResource]] =
    import quotes.reflect.*

    val annotations = TypeRepr.of[L].classSymbol.get.annotations

    Expr.ofList(
      annotations.view
        .map(_.asExpr)
        .filter(_.isExprOf[NeedsResource])
        .map(_.asExprOf[NeedsResource])
        .toList
    )

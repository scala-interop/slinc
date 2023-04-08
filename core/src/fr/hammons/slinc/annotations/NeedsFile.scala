package fr.hammons.slinc.annotations

import scala.annotation.StaticAnnotation
import fr.hammons.slinc.fset.Dependency
import java.nio.file.Paths
import scala.quoted.*

final case class NeedsFile(val path: String)
    extends StaticAnnotation,
      DependencyAnnotation:
  def toDependency: Dependency =
    val filePath = Paths.get(path).nn
    Dependency.FilePath(filePath)

object NeedsFile:
  inline def apply[F]: List[NeedsFile] = ${
    apply[F]()
  }

  private def apply[F]()(using Quotes, Type[F]): Expr[List[NeedsFile]] =
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
          case n @ '{ new NeedsFile(${ _ }) } => n
        .toList
    )

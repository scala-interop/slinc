package fr.hammons.slinc

import scala.annotation.StaticAnnotation
import scala.quoted.*

class LibraryName(name: String) extends StaticAnnotation

enum LibraryLocation:
  case Local(s: String)
  case Resource(s: String)
  case Path(s: String)

object LibraryName:
  def libraryName[L](using Quotes, Type[L]) =
    import quotes.reflect.*
    val classSymbol = MacroHelpers.getClassSymbol[L]
    classSymbol.annotations.collect {
      case Apply(
            Select(New(TypeIdent("LibraryName")), "<init>"),
            List(Literal(StringConstant(name)))
          ) =>
        name
    }.map{
      case s"@$path" => LibraryLocation.Resource(path)
      case s"#$path" => LibraryLocation.Path(path)
      case s => LibraryLocation.Local(s)
    }.headOption

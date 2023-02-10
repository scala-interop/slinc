package fr.hammons.slinc

import scala.annotation.StaticAnnotation
import scala.quoted.*
import scala.annotation.nowarn

class LibraryName(val name: String) extends StaticAnnotation

//todo: remove this once https://github.com/lampepfl/dotty/issues/16878 is fixed
@nowarn("msg=unused explicit parameter")
enum LibraryLocation:
  case Local(s: String)
  case Resource(s: String)
  case Path(s: String)
  case Standard

object LibraryName:
  def libraryName[L](using Quotes, Type[L]) =
    import quotes.reflect.*
    val classSymbol = MacroHelpers.getClassSymbol[L]
    classSymbol.annotations
      .collect {
        case Apply(
              Select(New(TypeIdent("LibraryName")), "<init>"),
              List(Literal(StringConstant(name)))
            ) =>
          name
      }
      .map {
        case s"@$path" => LibraryLocation.Resource(path)
        case s"#$path" => LibraryLocation.Path(path)
        case s         => LibraryLocation.Local(s)
      }
      .headOption
      .getOrElse(LibraryLocation.Standard)

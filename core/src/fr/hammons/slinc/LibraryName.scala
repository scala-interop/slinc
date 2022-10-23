package fr.hammons.slinc

import scala.annotation.StaticAnnotation
import scala.quoted.*

class LibraryName(name: String) extends StaticAnnotation

object LibraryName: 
  def libraryName[L](using Quotes, Type[L]) =
    import quotes.reflect.*
    val classSymbol = MacroHelpers.getClassSymbol[L]
    classSymbol.annotations.collect{
      case Apply(
        Select(New(TypeIdent("LibraryName")), "<init>"),
        List(Literal(StringConstant(name)))
      ) => name
    }.headOption
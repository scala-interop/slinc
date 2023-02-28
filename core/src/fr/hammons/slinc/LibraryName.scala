package fr.hammons.slinc

import scala.annotation.StaticAnnotation
import scala.quoted.*
import scala.annotation.nowarn
import fr.hammons.slinc.types.Arch
import fr.hammons.slinc.types.OS
class LibraryName(val name: String) extends StaticAnnotation

//todo: remove this once https://github.com/lampepfl/dotty/issues/16878 is fixed
@nowarn("msg=unused explicit parameter")
enum LibraryLocation:
  case Local(s: String)
  case Resource(name:String,candidates: List[String])
  case Path(s: String)
  case Standard
object LibraryLocation:
  object Resource:
    /**
     * infer shared library names from shorter name.
     * 
     * @param name 
     *   Simplified shared library name. For example, user can specify libfoo.so by `foo`.
     *   If name is quoted by backticks, SlinC uses that value without inference.
     *   For example "foo" is resolved to `List(libfoo${cpuarch.suffix}.so,libfoo.so,foo.so)` 
     *   whereas "`foo`" is resolved to `List(foo)`
    */
    def fromName(name: String): LibraryLocation.Resource =
      val candidates = name match
        case s"`$name`" =>
          List(name)
        case name =>
          (OS.inferred(), Arch.inferred()) match
            case (OS.Linux, cpuarch) =>
              List(s"lib$name${cpuarch.suffix}.so", s"lib$name.so", s"$name.so")
            case (OS.Darwin, cpuarch) =>
              List(
                s"lib$name${cpuarch.suffix}.dylib",
                s"lib$name${cpuarch.suffix}.so",
                s"lib$name.dylib",
                s"lib$name.so",
                s"$name.dylib",
                s"$name.so"
              )
            case (OS.Windows, cpuarch) =>
              List(
                s"lib$name${cpuarch.suffix}.dll",
                s"lib$name.dll",
                s"$name.dll"
              )
            case (OS.Unknown, cpuarch) =>
              List(s"lib$name${cpuarch.suffix}.so", s"lib$name.so")
      LibraryLocation.Resource(name,candidates)

object LibraryName:
  def libraryName[L](using Quotes, Type[L]): LibraryLocation =
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
        case s"@$name" => 
          LibraryLocation.Resource.fromName(name)
        case s"#$path" => LibraryLocation.Path(path)
        case s         => LibraryLocation.Local(s)
      }
      .headOption
      .getOrElse(LibraryLocation.Standard)

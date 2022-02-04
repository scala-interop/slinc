package io.gitlab.mhammons.slinc

import java.nio.file.Paths
import jdk.incubator.foreign.{
   SymbolLookup => JSymbolLookup,
   MemoryAddress,
   CLinker
}
import components.{SymbolLookup, Cache}
import scala.quoted.*
import scala.compiletime.{erasedValue, constValue}
import scala.annotation.tailrec

/** Denotes a collection of bindings to a library that's not part of the
  * standard library
  * @param location
  *   Where the .so file can be found.
  * @see
  *   [[io.gitlab.mhammons.slinc.Location]]
  */
@deprecated("Use one of the LibraryLocation subclasses instead", "v0.1.1")
trait Library(location: Location):
   location match
      case Location.Absolute(path) => System.load(path)
      case Location.Local(relPath) =>
         System.load(Paths.get(relPath).toAbsolutePath.toString)
      case Location.System(name) => System.loadLibrary(name)

   /** Tells the bindings where to look for your method
     */
   given SymbolLookup with
      val underlying = JSymbolLookup.loaderLookup
      def lookup(name: String) = underlying
         .lookup(name)
         .orElseThrow(() => new Exception(s"couldn't find symbol $name"))

class CLibrary[A](
    val cache: Cache,
    val lookup: JSymbolLookup
) //inline def call[R](args: Any*): R = ???
object CLibrary:
   inline def derived[A]: CLibrary[A] =
      val loader = inline erasedValue[A] match
         case _: LibraryLocation =>
            JSymbolLookup.loaderLookup
         case _ =>
            CLinker.systemLookup

      type prefix[A] = A match
         case WithPrefix[v] => v
         case _             => ""

      type rawCasing[A] = A match
         case RawNaming => true
         case _         => false

      CLibrary(Cache[A, prefix[A], rawCasing[A]](loader), loader)

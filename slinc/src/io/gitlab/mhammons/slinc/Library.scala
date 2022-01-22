package io.gitlab.mhammons.slinc

import java.nio.file.Paths
import jdk.incubator.foreign.{SymbolLookup => JSymbolLookup}
import components.SymbolLookup

/** Denotes a collection of bindings to a library that's not part of the
  * standard library
  * @param location
  *   Where the .so file can be found.
  * @see
  *   [[io.gitlab.mhammons.slinc.Location]]
  */
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

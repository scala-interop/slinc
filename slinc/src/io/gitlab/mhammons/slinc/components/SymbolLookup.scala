package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   SymbolLookup => JSymbolLookup,
   MemoryAddress,
   CLinker
}
import scala.jdk.OptionConverters.*

trait SymbolLookup:
   def lookup(name: String): MemoryAddress

object SymbolLookup:
   given SymbolLookup with
      val underlying = CLinker.systemLookup
      def lookup(name: String) = underlying
         .lookup(name)
         .toScala
         .getOrElse(
           throw new Exception(
             s"Couldn't find symbol $name in the C standard library"
           )
         )

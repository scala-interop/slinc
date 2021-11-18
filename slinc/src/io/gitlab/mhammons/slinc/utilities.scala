package io.gitlab.mhammons.slinc

import scala.quoted.*

import jdk.incubator.foreign.{CLinker, SymbolLookup, MemoryAddress}
import scala.jdk.OptionConverters.*

//todo: Change this. Lookup failure should not return match failure!!
val clookup: String => MemoryAddress =
   (s: String) =>
      CLinker.systemLookup
         .lookup(s)
         .toScala
         .orElse(SymbolLookup.loaderLookup.lookup(s).toScala)
         .getOrElse(throw new Exception(s"Couldn't find $s anywhere"))

def missingNativeCache(using Quotes) =
   import quotes.reflect.report
   report.errorAndAbort(
     "Couldn't find a NativeCache in the context. Please add `import slinc.NativeCache.given NativeCache` or provide your own NativeCache"
   )

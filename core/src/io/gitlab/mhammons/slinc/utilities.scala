package io.gitlab.mhammons.slinc

import scala.quoted.*

import jdk.incubator.foreign.CLinker
import scala.jdk.OptionConverters.*

//todo: Change this. Lookup failure should not return match failure!!
val clookup = Function.unlift(
  ((s: String) => CLinker.systemLookup.lookup(s))
     .andThen(_.toScala)
)

def missingNativeCache(using Quotes) =
   import quotes.reflect.report
   report.errorAndAbort(
     "Couldn't find a NativeCache in the context. Please add `import slinc.NativeCache.given NativeCache` or provide your own NativeCache"
   )

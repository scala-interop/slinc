package io.gitlab.mhammons.slinc.components

import scala.quoted.*

import jdk.incubator.foreign.{
   CLinker,
   SymbolLookup,
   MemoryAddress,
   SegmentAllocator
}
import scala.jdk.OptionConverters.*
import scala.compiletime.ops.boolean.!

val clookup: String => MemoryAddress =
   (s: String) =>
      CLinker.systemLookup
         .lookup(s)
         .toScala
         .orElse(SymbolLookup.loaderLookup.lookup(s).toScala)
         .getOrElse(throw new Exception(s"Couldn't find $s anywhere"))

def missingLayout[A: Type](using Quotes) =
   import quotes.reflect.report
   report.errorAndAbort(
     s"No layout for type ${Type.show[A]} was found. Please define one if you need this type."
   )

def missingTemplate[A: Type](using Quotes) =
   import quotes.reflect.report
   report.errorAndAbort(
     s"No template for type ${Type.show[A]} was found. Please define one if you need this type"
   )

extension (expr: Expr.type)
   def summonOrError[A: Type](using Quotes): Expr[A] =
      import quotes.reflect.{report}
      Expr
         .summon[A]
         .getOrElse(
           report.errorAndAbort(s"Could not summon ${Type.show[A]} in macro")
         )

type Allocates[A] = SegmentAllocator ?=> A

val segAlloc: Allocates[SegmentAllocator] = summon[SegmentAllocator]

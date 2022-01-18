package io.gitlab.mhammons.slinc.components

import scala.quoted.*

import jdk.incubator.foreign.{
   CLinker,
   SymbolLookup,
   MemoryAddress,
   MemorySegment,
   SegmentAllocator,
   ResourceScope
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

extension (expr: Expr.type)
   def summonOrError[A: Type](using Quotes): Expr[A] =
      import quotes.reflect.{report}
      Expr
         .summon[A]
         .getOrElse(
           report.errorAndAbort(s"Could not summon ${Type.show[A]} in macro")
         )

type Allocatee[A] = SegmentAllocator ?=> A

val segAlloc: Allocatee[SegmentAllocator] = summon[SegmentAllocator]

def allocate[A]: Allocatee[Informee[A, MemorySegment]] =
   segAlloc.allocate(layoutOf[A])

type Scopee[A] = ResourceScope ?=> A

val currentScope: Scopee[ResourceScope] = summon[ResourceScope]

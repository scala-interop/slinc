package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.CLinker.{C_INT, C_FLOAT, C_LONG, C_POINTER}
import scala.util.chaining.*

sealed trait MemLayout(val underlying: MemoryLayout)

enum Primitives(underlying: MemoryLayout) extends MemLayout(underlying):
   case Int extends Primitives(C_INT)
   case Float extends Primitives(C_FLOAT)
   case Long extends Primitives(C_LONG)
   case Pointer extends Primitives(C_POINTER)

case class StructLayout(layouts: (String, MemLayout)*)
    extends MemLayout(
      layouts
         .map((name, l) => l.underlying.withName(name))
         .pipe(MemoryLayout.structLayout(_*))
    )

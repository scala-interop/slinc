package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.CLinker.{
   C_INT,
   C_FLOAT,
   C_LONG,
   C_POINTER,
   C_DOUBLE
}
import scala.util.chaining.*

sealed trait MemLayout(val underlying: MemoryLayout):
   export underlying.{byteSize, varHandle}

   def withName(name: String) = Named(this, name)

case class Named[M <: MemLayout](memLayout: M, name: String)
    extends MemLayout(memLayout.underlying.withName(name))

enum Primitives(underlying: MemoryLayout) extends MemLayout(underlying):
   case Int extends Primitives(C_INT)
   case Float extends Primitives(C_FLOAT)
   case Long extends Primitives(C_LONG)
   case Pointer extends Primitives(C_POINTER)
   case Double extends Primitives(C_DOUBLE)

object Primitives:
   extension (p: Primitives) def withName(name: String) = Named(p, name)

case class StructLayout(layouts: Seq[Named[MemLayout]])
    extends MemLayout(
      layouts
         .map(_.underlying)
         .pipe(MemoryLayout.structLayout(_*))
    )

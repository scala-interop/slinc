package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.MemorySegment
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

//todo: MemorySegment, which carries layout information with it
case class StructLayout(layouts: Seq[Named[MemLayout]])
    extends MemLayout(
      layouts
         .map(_.underlying)
         .pipe(MemoryLayout.structLayout(_*))
    ):

   def byteOffset(name: String) =
      underlying.byteOffset(MemoryLayout.PathElement.groupElement(name))

   def subsegmntOf(name: String, memSegment: MemorySegment) =
      val pathCoords = MemoryLayout.PathElement.groupElement(name)
      val ptr = memSegment.address.addOffset(underlying.byteOffset(pathCoords))
      val subLayout = layouts.find(_.name == name).getOrElse(???)

      memSegment.address
         .addOffset(
           underlying.byteOffset(MemoryLayout.PathElement.groupElement(name))
         )
         .pipe(ptr => ptr.asSegment(subLayout.byteSize(), ptr.scope))

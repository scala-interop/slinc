package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.{MemorySegment, MemoryAddress}
import jdk.incubator.foreign.CLinker.{
   C_INT,
   C_FLOAT,
   C_LONG,
   C_POINTER,
   C_DOUBLE
}
import jdk.incubator.foreign.CLinker.toJavaString
import scala.util.chaining.*
import java.lang.invoke.VarHandle

sealed trait MemLayout(
    val underlying: MemoryLayout
):
   export underlying.{byteSize, varHandle}

   def withName(name: String) = Named(this, name)

case class Named[M <: MemLayout](memLayout: M, name: String)
    extends MemLayout(memLayout.underlying.withName(name))

enum Primitives(underlying: MemoryLayout, classRepr: Class[?])
    extends MemLayout(underlying):
   case Int
       extends Primitives(
         C_INT,
         classOf[Int]
       )
   case Float
       extends Primitives(
         C_FLOAT,
         classOf[Float]
       )
   case Long
       extends Primitives(
         C_LONG,
         classOf[Long]
       )
   case Double
       extends Primitives(
         C_DOUBLE,
         classOf[Double]
       )
   def repr = classRepr
object Primitives:
   extension (p: Primitives) def withName(name: String) = Named(p, name)

   val intVh = C_INT.varHandle(classOf[Int])
   val floatVh = C_FLOAT.varHandle(classOf[Float])
   val longVh = C_LONG.varHandle(classOf[Long])
   val doubleVh = C_DOUBLE.varHandle(classOf[Double])

case class Pointer(derefsTo: MemLayout)
    extends MemLayout(C_POINTER)

case object Str extends MemLayout(C_POINTER)

//todo: MemorySegment, which carries layout information with it
case class StructLayout(layouts: Seq[Named[MemLayout]])
    extends MemLayout(
      layouts
         .map(_.underlying)
         .pipe(MemoryLayout.structLayout(_*))
    ):

   lazy val layoutsMap =
      layouts.map(named => named.name -> named.memLayout).toMap

   def byteOffset(name: String) =
      underlying.byteOffset(MemoryLayout.PathElement.groupElement(name))

   def subsegmntOf(name: String, memSegment: MemorySegment) =
      val pathCoords = MemoryLayout.PathElement.groupElement(name)
      val ptr = memSegment.address.addOffset(underlying.byteOffset(pathCoords))
      val subLayout = layoutsMap(name)

      ptr.asSegment(subLayout.byteSize(), ptr.scope)

   def varhandleOf(name: String) =
      layoutsMap(name) match
         case p: Primitives =>
            underlying.varHandle(
              p.repr,
              MemoryLayout.PathElement.groupElement(name)
            )
         case _ => ???

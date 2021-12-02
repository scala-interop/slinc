package io.gitlab.mhammons.slinc.components

import java.lang.invoke.VarHandle

import jdk.incubator.foreign.{MemorySegment, MemoryLayout},
MemoryLayout.PathElement
import scala.reflect.ClassTag
import io.gitlab.mhammons.polymorphics.VarHandleHandler

class PrimitiveTemplate[T <: AnyVal: ClassTag](
    val layout: MemoryLayout,
    val path: Seq[PathElement],
    varHandle: VarHandle
) extends SegmentTemplate[Member[T]]:
   def apply(memorySegment: MemorySegment) =
      Member[T](memorySegment, varHandle, layout, path)(using this)
   def subTemplate(memoryLayout: MemoryLayout, path: Seq[PathElement]) =
      PrimitiveTemplate[T](
        memoryLayout,
        path,
        memoryLayout.varHandle(summon[ClassTag[T]].runtimeClass, path*)
      )

class IntTemplate(val layout: MemoryLayout, varHandle: VarHandle)
    extends Template[Int]:
   def path = ???
   def apply(memorySegment: MemorySegment) =
      VarHandleHandler.get(varHandle, memorySegment).asInstanceOf[Int]
   def subTemplate(memoryLayout: MemoryLayout, path: Seq[PathElement]) =
      IntTemplate(memoryLayout, memoryLayout.varHandle(classOf[Int], path*))

// class LongTemplate(val layout: MemoryLayout, val path: Seq[PathElement], varHandle: VarHandle)
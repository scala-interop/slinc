package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.{MemorySegment, MemoryLayout},
MemoryLayout.PathElement
import io.gitlab.mhammons.slinc.{Struct, MapStruct}
import io.gitlab.mhammons.slinc.components.{MemLayout, Member}
import scala.quoted.*
import io.gitlab.mhammons.slinc.StructMacros
import scala.collection.immutable.ArraySeq

class StructTemplate[A <: Struct](
    names: ArraySeq[String],
    templates: ArraySeq[Template[?]],
    val layout: MemoryLayout,
    val path: Seq[PathElement]
) extends SegmentTemplate[A],
      Allocatable[A]:
   def apply(memorySegment: MemorySegment) =
      val builder = Map.newBuilder[String, Any]

      var i = 0
      while i < names.length do
         builder.addOne(names(i) -> templates(i)(memorySegment))
         i += 1

      MapStruct(
        builder.result,
        memorySegment,
        layout,
        path
      ).asInstanceOf[A]
   def subTemplate(
       memoryLayout: MemoryLayout,
       path: Seq[PathElement]
   ) = new StructTemplate[A](
     names,
     (0 until templates.size)
        .map(idx =>
           templates(idx).subTemplate(
             memoryLayout,
             path :+ PathElement.groupElement(names(idx))
           )
        )
        .to(ArraySeq),
     memoryLayout,
     path
   )
   override def toString = layout.toString

   def allocate(using seg: SegmentAllocator) = apply(seg.allocate(this.layout))

object StructTemplate:
   def apply[A <: Struct](
       kvs: Seq[(String, Template[?])],
       layout: MemoryLayout
   ) =
      val (names, templates) = kvs
         .map((name, template) =>
            (
              name,
              template.subTemplate(layout, Seq(PathElement.groupElement(name)))
            )
         )
         .unzip
      new StructTemplate[A](
        names.to(ArraySeq),
        templates.to(ArraySeq),
        layout,
        Nil
      )

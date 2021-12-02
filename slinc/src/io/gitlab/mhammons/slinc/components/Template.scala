package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryLayout, CLinker},
MemoryLayout.PathElement,
CLinker.{C_INT, C_FLOAT, C_DOUBLE, C_CHAR, C_LONG, C_POINTER, C_SHORT}

trait Template[T]:
   def layout: MemoryLayout
   def path: Seq[PathElement]
   def apply(memSegment: MemorySegment): T
   def subTemplate(
       memLayout: MemoryLayout,
       path: Seq[PathElement]
   ): Template[T]

trait SegmentTemplate[T] extends Template[T]

object Template:
   given Template[Int] =
      IntTemplate(C_INT, C_INT.varHandle(classOf[Int]))

   given SegmentTemplate[Member[Int]] = PrimitiveTemplate[Int](
     C_INT,
     Nil,
     C_INT.varHandle(classOf[Int])
   )
   given memberFloat: SegmentTemplate[Member[Float]] = PrimitiveTemplate[Float](
     C_FLOAT,
     Nil,
     C_FLOAT.varHandle(classOf[Float])
   )

   given memberLong: SegmentTemplate[Member[Long]] = PrimitiveTemplate[Long](
     C_LONG,
     Nil,
     C_LONG.varHandle(classOf[Long])
   )

   given SegmentTemplate[Nothing] with
      def path = ???
      def layout = ???
      def apply(memSegment: MemorySegment) = ???
      def subTemplate(memLayout: MemoryLayout, path: Seq[PathElement]) = ???

package io.gitlab.mhammons.slinc

import jdk.incubator.foreign,
foreign.{MemoryLayout, MemorySegment, ResourceScope, SegmentAllocator, CLinker}
import javax.swing.text.Segment
import cats.{~>, Id}
import java.lang.invoke.MethodHandle

enum NativeOp[A]:
   case Allocate[A](
       aName: String,
       layout: MemoryLayout,
       structMaker: (MemorySegment) => A,
       allocator: SegmentAllocator
   ) extends NativeOp[A]
   case Scope[A](result: (ResourceScope, SegmentAllocator) => NativeIO[A])
       extends NativeOp[A]
   case Unit extends NativeOp[Unit]
   case Pure[A](value: () => A) extends NativeOp[A]
   case Layout(
       name: String,
       layoutGen: Map[String, MemoryLayout] => NativeIO[
         Map[String, MemoryLayout]
       ]
   ) extends NativeOp[MemoryLayout]
   case MethodHandleBinding[A](
       name: String,
       generator: CLinker => NativeIO[MethodHandle]
   ) extends NativeOp[MethodHandle]

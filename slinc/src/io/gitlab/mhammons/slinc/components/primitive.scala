package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryAddress}
import java.lang.invoke.VarHandle
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import jdk.incubator.foreign.SegmentAllocator

class primitive[A <: AnyVal](
    memorySegment: MemorySegment,
    addr: MemoryAddress,
    varhandle: VarHandle,
    layout: Primitives
):
   def apply() = VarHandleHandler.get(varhandle, memorySegment).asInstanceOf[A]
   def update(a: A) = VarHandleHandler.set(varhandle, memorySegment, a)
   def `unary_~` =
      Ptr(
        addr,
        layout.byteSize(),
        primitive[A](_, addr, varhandle, layout)
      )

type int = primitive[Int]
object int:
   def apply(
       memorySegment: MemorySegment,
       addr: MemoryAddress,
       varhandle: VarHandle
   ): int = primitive[Int](memorySegment, addr, varhandle, Primitives.Int)
   def allocate(using seg: SegmentAllocator) =
      val segment = seg.allocate(Primitives.Int.underlying)
      Primitives.Int.template(segment).asInstanceOf[int]

type float = primitive[Float]
object float:
   def apply(
       memorySegment: MemorySegment,
       addr: MemoryAddress,
       varhandle: VarHandle
   ): float = primitive[Float](memorySegment, addr, varhandle, Primitives.Float)

type long = primitive[Long]
object long:
   def apply(
       memorySegment: MemorySegment,
       addr: MemoryAddress,
       varhandle: VarHandle
   ): long = primitive[Long](memorySegment, addr, varhandle, Primitives.Long)

type double = primitive[Double]
object double:
   def apply(
       memorySegment: MemorySegment,
       addr: MemoryAddress,
       varHandle: VarHandle
   ): double =
      primitive[Double](memorySegment, addr, varHandle, Primitives.Double)

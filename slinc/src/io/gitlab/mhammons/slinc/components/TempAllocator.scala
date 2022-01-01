package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemorySegment,
   SegmentAllocator,
   CLinker,
   ResourceScope,
   MemoryAddress
}
import java.lang.ref.Reference
import scala.collection.mutable.Seq

private val allocationLocal = ThreadLocal[MemorySegment]()

private def reallocateSts(bytesNeeded: Long, old: MemorySegment) =
   val size = powersOf2.dropWhile(_ < bytesNeeded).head
   val address = CLinker
      .allocateMemory(size)
      .asSegment(size, ResourceScope.globalScope)
   if old != null then
      address.copyFrom(old)
      CLinker.freeMemory(old.address)
   allocationLocal.set(address)
   address

private val powersOf2 = LazyList.iterate(1)(_ << 1)

class TempAllocator:
   val powerIter = powersOf2.iterator
   var currentSegment = allocationLocal.get
   var currentOffset = 0L
   var toFree = Seq.empty[MemoryAddress]

   val localAllocator: SegmentAllocator = (bytesNeeded, alignment) =>
      val totalBytesNeeded = currentOffset + bytesNeeded
      if currentSegment == null || currentSegment.byteSize < totalBytesNeeded then
         println(
           s"reallocating to fit $bytesNeeded cause offset is at $currentOffset"
         )
         if currentSegment != null then toFree :+= currentSegment.address
         currentOffset = 0L
         var nextSize = powerIter.next
         while nextSize < bytesNeeded do nextSize = powerIter.next
         currentSegment = CLinker
            .allocateMemory(nextSize)
            .asSegment(nextSize, ResourceScope.globalScope)

      val result = currentSegment.asSlice(currentOffset, bytesNeeded)
      currentOffset += bytesNeeded
      result

   def close() =
      toFree.foreach(CLinker.freeMemory)
      allocationLocal.set(currentSegment)

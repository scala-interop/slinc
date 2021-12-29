package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemorySegment,
   SegmentAllocator,
   CLinker,
   ResourceScope
}

private val allocationLocal = ThreadLocal[MemorySegment]()

def reallocateSts(bytesNeeded: Long, old: MemorySegment) =
   val size = powersOf2.dropWhile(_ < bytesNeeded).head
   val address = CLinker
      .allocateMemory(size)
      .asSegment(size, ResourceScope.globalScope)
   if old != null then CLinker.freeMemory(old.address)
   allocationLocal.set(address)
   address


val powersOf2 = LazyList.iterate(1)(_ << 1)


val localAllocator: SegmentAllocator = (bytesNeeded, alignment) =>
   val maybeSts = allocationLocal.get

   val sts =
      if maybeSts == null then reallocateSts(bytesNeeded, null)
      else maybeSts

   val usableAddress = if sts.byteSize < bytesNeeded then
      println(s"reallocating to $bytesNeeded")
      reallocateSts(bytesNeeded, sts)
   else sts

   usableAddress

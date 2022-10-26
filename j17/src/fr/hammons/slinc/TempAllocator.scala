package fr.hammons.slinc

import jdk.incubator.foreign.{
  CLinker,
  ResourceScope,
  MemoryAddress,
  MemorySegment,
  SegmentAllocator
}

final class TempAllocator17:
  private val powersOf2 = LazyList.iterate(1L)(_ << 1).iterator
  private var currentSegment =
    val next = powersOf2.next
    CLinker
      .allocateMemory(next)
      .nn
      .asSegment(next, ResourceScope.globalScope.nn)
      .nn
  private var offset = 0L
  private var rs: (() => Unit) | Null = null

  private def addToRs(memoryAddress: MemoryAddress) =
    if rs != null then
      val curRs = rs
      rs = () => { curRs.nn(); CLinker.freeMemory(memoryAddress) }
    else rs = () => CLinker.freeMemory(memoryAddress)

  def allocate(bytes: Long, alignment: Long): MemorySegment =
    if bytes + offset > currentSegment.byteSize then
      addToRs(currentSegment.address.nn)
      offset = 0L
      var nextSize = powersOf2.next
      while nextSize < bytes do nextSize = powersOf2.next
      currentSegment = MemorySegment
        .allocateNative(nextSize, alignment, ResourceScope.globalScope)
        .nn

      allocate(bytes, alignment)
    else
      val oldOffset = offset
      offset += bytes
      currentSegment.asSlice(oldOffset, bytes).nn

  def reset() =
    offset = 0L
    if rs != null then
      rs.nn()
      rs = null

private[slinc] object TempAllocator17:
  val allocator = ThreadLocal.withInitial(() => TempAllocator17()).nn
//   def reset() = allocator.get.nn.reset()
//   def localAllocator(): SegmentAllocator =
//    val all = allocator.get().nn
//    (bytesNeeded, alignment) => all.allocate(bytesNeeded, alignment)
  val powersOf2 = LazyList.iterate(1L)(_ << 1)

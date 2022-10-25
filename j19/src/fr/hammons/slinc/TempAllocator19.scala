package fr.hammons.slinc

import java.lang.foreign.{MemorySession, MemorySegment}
import java.lang.foreign.MemoryAddress
import java.lang.foreign.SegmentAllocator

final class TempAllocator19:
  private val powersOf2 = LazyList.iterate(1L)(_ << 1).iterator
  private var ms: MemorySession = MemorySession.openConfined().nn 
  private var gc: MemorySession | Null = null 

  private var currentSegment = 
    val next = powersOf2.next     
    ms.allocate(next).nn

  private var offset = 0L

  private def addToGc(ms: MemorySession) = 
    if gc == null then 
      gc = ms 
    else 
      gc.nn.addCloseAction(() => ms.close())

  def allocate(bytes: Long, alignment: Long): MemorySegment =
    if bytes + offset > currentSegment.byteSize() then 
      offset = 0L
      var nextSize = powersOf2.next 
      while nextSize < bytes do nextSize = powersOf2.next 
      currentSegment = 
        addToGc(ms)
        ms = MemorySession.openConfined().nn
        ms.allocate(nextSize, alignment).nn
      allocate(bytes, alignment)
    else 
      val oldOffset = offset
      offset += bytes
      currentSegment.asSlice(oldOffset, bytes).nn

  def reset() =
    offset = 0l 
    if gc != null then 
      gc.nn.close()
      gc = null 

object TempAllocator19:
  private val allocator = ThreadLocal.withInitial(() => TempAllocator19()).nn
  def reset() = allocator.get.nn.reset()
  val localAllocator: SegmentAllocator = 
    (bytesNeeded, alignment) => allocator.get.nn.allocate(bytesNeeded, alignment)
  private val powersOf2 = LazyList.iterate(1l)(_ << 1)


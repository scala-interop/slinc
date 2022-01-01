package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemoryAccess, MemorySegment}
object MemAccess {
   def getIntAtOffset(memorySegment: MemorySegment, offset: Long) =
      MemoryAccess.getIntAtOffset(memorySegment, offset)
}

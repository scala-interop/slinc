package fr.hammons.sffi

import jdk.incubator.foreign.{MemorySegment, MemoryAccess}

object Assign172 extends AssignI[MemorySegment]:
  given intAssign: Assign[Int] with
    def assign(mem: MemorySegment, offset: Long, a: Int): Unit =
      MemoryAccess.setIntAtOffset(mem, offset, a)

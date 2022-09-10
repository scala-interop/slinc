package fr.hammons.sffi

import jdk.incubator.foreign.{MemorySegment, MemoryAccess}

object Deref172 extends DerefI[MemorySegment]:
  given intDeref: Deref[Int] with
    def deref(mem: MemorySegment, offset: Long): Int =
      MemoryAccess.getIntAtOffset(mem, offset)

    def toArray(mem: MemorySegment, offset: Long, elements: Long): Array[Int] =
      ???

  given byteDeref: Deref[Byte] with
    def deref(mem: MemorySegment, offset: Long): Byte =
      MemoryAccess.getByteAtOffset(mem, offset)

    def toArray(mem: MemorySegment, offset: Long, elements: Long): Array[Byte] =
      ???

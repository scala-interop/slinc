package fr.hammons.sffi

import jdk.incubator.foreign.ResourceScope
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.MemoryAccess
import jdk.incubator.foreign.MemorySegment
import scala.annotation.targetName

object Allocatable172 extends AllocatableI[ResourceScope, SegmentAllocator, MemorySegment]:
  given byteIsAllocatable: Allocatable[Byte] with 
    def apply(a: Byte)(using ResourceScope, SegmentAllocator): MemorySegment = 
      ???
    @targetName("arrayApply")
    def apply(a: Array[Byte])(using ResourceScope, SegmentAllocator): MemorySegment = ???

  given intIsAllocatable: Allocatable[Int] with 
    def apply(a: Int)(using ResourceScope, SegmentAllocator): MemorySegment =   
      val mem = summon[SegmentAllocator].allocate(CLinker.C_INT)
      MemoryAccess.setInt(mem, a)
      mem.nn
    @targetName("arrayApply")
    def apply(a: Array[Int])(using ResourceScope, SegmentAllocator): MemorySegment = ???
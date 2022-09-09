package fr.hammons.sffi

import jdk.incubator.foreign.{
  MemoryLayout,
  ResourceScope,
  MemorySegment,
  SegmentAllocator
}

trait Basics17 extends WBasics:
  type Context = MemoryLayout
  type Scope = ResourceScope
  type RawMem = MemorySegment
  type Allocator = SegmentAllocator

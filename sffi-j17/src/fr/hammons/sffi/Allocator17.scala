package fr.hammons.sffi

import jdk.incubator.foreign.{SegmentAllocator, ResourceScope}

class Allocator17(segmentAllocator: SegmentAllocator) extends Allocator:
  def allocate(layout: DataLayout): Mem =
    Mem17(segmentAllocator.allocate(layout.size.toLong).nn)
  def upcall[Fn](function: Fn): Mem = ???

object Allocator17 extends Allocator.PlatformSpecific:
  def globalAllocator() =
    Allocator17(SegmentAllocator.arenaAllocator(ResourceScope.globalScope()).nn)

  def tempAllocator(): Allocator = ???

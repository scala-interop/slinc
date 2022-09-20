package fr.hammons.slinc

import jdk.incubator.foreign.{SegmentAllocator, ResourceScope}

class Allocator17(
    segmentAllocator: SegmentAllocator,
    scope: ResourceScope,
    canClose: Boolean
) extends Allocator:
  override def allocate(layout: DataLayout): Mem = 
    Mem17(segmentAllocator.allocate(layout.size.toLong, layout.alignment.toLong).nn)

  override def upcall[Fn](function: Fn): Mem = ???

  override def close(): Unit = if canClose then scope.close()

object Allocator17 extends Allocator.PlatformSpecific:
  def globalAllocator(): Allocator =
    val global = ResourceScope.globalScope().nn
    Allocator17(SegmentAllocator.arenaAllocator(global).nn, global, false)

  def tempAllocator(): Allocator = ???

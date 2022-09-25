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
  override def base: Object = segmentAllocator


object Scope17 extends ScopeI.PlatformSpecific:
  def createGlobalScope: GlobalScope = new GlobalScope:
    def apply[A](fn: (Allocator) ?=> A): A = 
      val rs = ResourceScope.globalScope().nn
      given Allocator = Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, false)
      fn 

  def createConfinedScope: ConfinedScope = new ConfinedScope:
    def apply[A](fn: Allocator ?=> A): A = 
      val rs = ResourceScope.newConfinedScope().nn
      given Allocator = Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, false)
      val res = fn 
      rs.close()
      res
    
  def createTempScope: TempScope = new TempScope:
    def apply[A](fn: Allocator ?=> A): A =
      given Allocator = Allocator17(TempAllocator.localAllocator(), ResourceScope.globalScope().nn, false)
      val res = fn 
      TempAllocator.reset()
      res



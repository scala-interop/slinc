package fr.hammons.slinc

import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.ResourceScope
import jdk.incubator.foreign.SegmentAllocator

class Scope17(layoutI: LayoutI, linker: CLinker) extends ScopeI.PlatformSpecific(layoutI):
  def createGlobalScope: GlobalScope = new GlobalScope:
    def apply[A](fn: (Allocator) ?=> A): A =
      val rs = ResourceScope.globalScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker, layoutI)
      fn

  def createConfinedScope: ConfinedScope = new ConfinedScope:
    def apply[A](fn: Allocator ?=> A): A =
      val rs = ResourceScope.newConfinedScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker, layoutI)
      val res = fn
      rs.close()
      res

  def createTempScope: TempScope = new TempScope:
    def apply[A](fn: Allocator ?=> A): A =
      val allocator = TempAllocator17.allocator.get().nn
      val segmentAllocator: SegmentAllocator = (bytesNeeded, alignment) => allocator.allocate(bytesNeeded, alignment)
      given Allocator = Allocator17(
        segmentAllocator,
        ResourceScope.globalScope().nn,
        linker,
        layoutI
      )
      val res = fn
      allocator.reset()
      res

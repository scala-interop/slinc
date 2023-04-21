package fr.hammons.slinc

import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.ResourceScope
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.MemoryAddress

class Scope17(linker: CLinker) extends ScopeI.PlatformSpecific:

  private val baseNull = Ptr[Nothing](
    Mem17(MemoryAddress.NULL.nn.asSegment(1, ResourceScope.globalScope).nn),
    Bytes(0)
  )

  def nullPtr[A] = baseNull.castTo[A]

  def createGlobalScope: GlobalScope = new GlobalScope:
    def apply[A](fn: (Allocator) ?=> A): A =
      val rs = ResourceScope.globalScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker)
      fn

  def createConfinedScope: ConfinedScope = new ConfinedScope:
    def apply[A](fn: Allocator ?=> A): A =
      val rs = ResourceScope.newConfinedScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker)
      val res = fn
      rs.close()
      res

  def createSharedScope: SharedScope = new SharedScope:
    def apply[A](fn: Allocator ?=> A): A =
      val rs = ResourceScope.newSharedScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker)
      val res = fn
      rs.close()
      res

  def createTempScope: TempScope = new TempScope:
    def apply[A](fn: Allocator ?=> A): A =
      val allocator = TempAllocator17.allocator.get().nn
      val segmentAllocator: SegmentAllocator = (bytesNeeded, alignment) =>
        allocator.allocate(bytesNeeded, alignment)
      given Allocator = Allocator17(
        segmentAllocator,
        ResourceScope.globalScope().nn,
        linker
      )
      val res = fn
      allocator.reset()
      res

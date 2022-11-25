package fr.hammons.slinc

import java.lang.foreign.Linker
import java.lang.foreign.MemorySession
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemoryAddress

class Scope19(layoutI: LayoutI, linker: Linker)
    extends ScopeI.PlatformSpecific(layoutI):

  val baseNull: Ptr[Any] = Ptr(
    Mem19(
      MemorySegment.ofAddress(MemoryAddress.NULL, 0, MemorySession.global()).nn
    ),
    Bytes(0)
  )
  override def nullPtr[A]: Ptr[A] = baseNull.castTo[A]

  override def createTempScope: TempScope = new TempScope:
    given Allocator = Allocator19(
      TempAllocator19.localAllocator,
      MemorySession.global().nn,
      linker,
      layoutI
    )
    def apply[A](fn: Allocator ?=> A): A =
      val res = fn
      TempAllocator19.reset()
      res

  override def createGlobalScope: GlobalScope = new GlobalScope:
    def apply[A](fn: Allocator ?=> A): A =
      val rs = MemorySession.global().nn
      given Allocator =
        Allocator19(SegmentAllocator.newNativeArena(rs).nn, rs, linker, layoutI)
      fn

  override def createConfinedScope: ConfinedScope = new ConfinedScope:
    def apply[A](fn: Allocator ?=> A): A =
      val rs = MemorySession.openConfined().nn
      given Allocator =
        Allocator19(SegmentAllocator.newNativeArena(rs).nn, rs, linker, layoutI)
      val res = fn
      rs.close()
      res

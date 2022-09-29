package fr.hammons.slinc

import java.lang.foreign.Linker
import java.lang.foreign.MemorySession
import java.lang.foreign.SegmentAllocator

class Scope19(layoutI: LayoutI) extends ScopeI.PlatformSpecific(layoutI):
  val linker = Linker.nativeLinker().nn

  override def createTempScope: TempScope = new TempScope:
    def apply[A](fn: Allocator ?=> A): A =
      given Allocator = Allocator19(
        TempAllocator.localAllocator(),
        MemorySession.global().nn,
        linker,
        layoutI
      )
      val res = fn
      TempAllocator.reset()
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

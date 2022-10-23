package fr.hammons.slinc

import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.MemoryAddress

object Transitions19 extends TransitionsI.PlatformSpecific:
  def outStruct(obj: Object, size: Bytes): Mem = Mem19(
    obj.asInstanceOf[MemorySegment]
  )

  val inMem: InTransitionNeeded[Mem] = new InTransitionNeeded[Mem]: 
    def in(a: Mem): Object = a.asBase

  val outMem: OutTransitionNeeded[Mem] = new OutTransitionNeeded[Mem]:
    def out(obj: Object): Mem = Mem19(obj.asInstanceOf[MemorySegment])

  val inPointer: InTransitionNeeded[Mem] = new InTransitionNeeded[Mem]:
    def in(a: Mem): Object = a.asBase.asInstanceOf[MemorySegment].address().nn

  val outPointer: OutTransitionNeeded[Mem] = new OutTransitionNeeded[Mem]:
    import scala.language.unsafeNulls
    def out(obj: Object): Mem = Mem19(
      MemorySegment.ofAddress(obj.asInstanceOf[MemoryAddress], Int.MaxValue, MemorySession.global())
    )
  
  val allocatorIn: InTransitionNeeded[Allocator] = new InTransitionNeeded[Allocator]:
    def in(a: Allocator): Object = a.base
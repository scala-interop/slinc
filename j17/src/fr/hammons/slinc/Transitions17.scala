package fr.hammons.slinc

import jdk.incubator.foreign.{MemorySegment, MemoryAddress, ResourceScope}

object Transitions17 extends TransitionsI.PlatformSpecific:

  def outStruct(obj: Object, size: Bytes): Mem = Mem17(obj.asInstanceOf[MemorySegment])
  val inMem: InTransitionNeeded[Mem] = new InTransitionNeeded[Mem]:
    def in(a: Mem): Object = a.asBase

  val outMem: OutTransitionNeeded[Mem] = new OutTransitionNeeded[Mem]:
    def out(obj: Object): Mem = Mem17(obj.asInstanceOf[MemorySegment])

  val inPointer: InTransitionNeeded[Mem] = new InTransitionNeeded[Mem]:
    def in(a: Mem): Object = a.asBase.asInstanceOf[MemorySegment].address().nn

  val outPointer: OutTransitionNeeded[Mem] = new OutTransitionNeeded[Mem]:
    def out(obj: Object): Mem = Mem17(
      obj
        .asInstanceOf[MemoryAddress]
        .asSegment(1, ResourceScope.globalScope().nn)
        .nn
    )

  val allocatorIn: InTransitionNeeded[Allocator] =
    new InTransitionNeeded[Allocator]:
      def in(a: Allocator): Object = a.base

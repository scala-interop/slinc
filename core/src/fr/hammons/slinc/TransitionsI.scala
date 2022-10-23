package fr.hammons.slinc

class TransitionsI(platformSpecific: TransitionsI.PlatformSpecific):
  private val genPtrInTransition = new InTransitionNeeded[Ptr[Any]]:
    def in(a: Ptr[Any]) =
      platformSpecific.inPointer.in(a.mem.offset(a.offset))

  private val genPtrOutTransition = new OutTransitionNeeded[Ptr[Any]]:
    def out(a: Object): Ptr[Any] =
      Ptr[Any](platformSpecific.outPointer.out(a), Bytes(0))      
  
  given [A]: InTransitionNeeded[Ptr[A]] =
    genPtrInTransition.asInstanceOf[InTransitionNeeded[Ptr[A]]]

  given [A]: OutTransitionNeeded[Ptr[A]] =
    genPtrOutTransition.asInstanceOf[OutTransitionNeeded[Ptr[A]]]
  given InTransitionNeeded[Allocator] = platformSpecific.allocatorIn

  private[slinc] def structMemIn(mem: Mem)(using Allocator) =
    platformSpecific.inMem.in(mem)
  private[slinc] def structMemOut(o: Object) = platformSpecific.outMem.out(o)

object TransitionsI:
  trait PlatformSpecific:
    val inMem: InTransitionNeeded[Mem]
    val outMem: OutTransitionNeeded[Mem]
    val inPointer: InTransitionNeeded[Mem]
    val outPointer: OutTransitionNeeded[Mem]
    val allocatorIn: InTransitionNeeded[Allocator]

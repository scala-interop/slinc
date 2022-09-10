package fr.hammons.sffi

import jdk.incubator.foreign.SegmentAllocator

object InTransition172 extends InTransitionI[SegmentAllocator, LayoutInfo172.LayoutInfo]:
  given intInTransition: InTransition[Int] with
    def to(a: Int): (SegmentAllocator) ?=> Object = a.asInstanceOf[Object]

  given byteInTransition: InTransition[Byte] with 
    def to(a: Byte): (SegmentAllocator) ?=> Object = a.asInstanceOf[Object]

  given floatInTransition: InTransition[Float] with 
    def to(a: Float): (SegmentAllocator) ?=> Object = a.asInstanceOf[Object]

  given longInTransition: InTransition[Long] with 
    def to(a: Long): (SegmentAllocator) ?=> Object = a.asInstanceOf[Object]
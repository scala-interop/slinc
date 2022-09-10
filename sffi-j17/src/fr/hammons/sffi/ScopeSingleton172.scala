package fr.hammons.sffi

import jdk.incubator.foreign.ResourceScope
import jdk.incubator.foreign.SegmentAllocator

object ScopeSingleton172
    extends ScopeSingleton[ResourceScope, SegmentAllocator]:
  def apply[T](
      allocatorType: AllocatorType,
      shared: Boolean,
      global: Boolean
  )(code: ResourceScope ?=> SegmentAllocator ?=> T): T =
    val resourceScope =
      if global then ResourceScope.globalScope().nn
      else if shared then ResourceScope.newSharedScope().nn
      else ResourceScope.newConfinedScope().nn

    given segmentAllocator: SegmentAllocator = allocatorType match
      case AllocatorType.Arena =>
        SegmentAllocator.arenaAllocator(resourceScope).nn
      case AllocatorType.Implicit =>
        SegmentAllocator.ofScope(resourceScope).nn

    given ResourceScope = resourceScope

    val res = code

    if !global then resourceScope.close()
    res

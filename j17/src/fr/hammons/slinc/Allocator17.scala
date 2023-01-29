package fr.hammons.slinc

import jdk.incubator.foreign.{
  SegmentAllocator,
  ResourceScope,
  CLinker,
  FunctionDescriptor
}
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.MethodHandle
import dotty.tools.dotc.transform.init.Semantic.Fun
import fr.hammons.slinc.modules.DescriptorModule

class Allocator17(
    segmentAllocator: SegmentAllocator,
    scope: ResourceScope,
    linker: CLinker,
    layoutI: LayoutI
)(using dm: DescriptorModule) extends Allocator(layoutI):
  import layoutI.*

  override def upcall[Fn](descriptor: Descriptor, target: Fn): Mem =
    val size = descriptor.inputDescriptors.size
    val mh = methodHandleFromFn(descriptor, target)
    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        FunctionDescriptor.of(
          LayoutI17.dataLayout2MemoryLayout(dm.toDataLayout(r)),
          descriptor.inputDescriptors.view.map(dm.toDataLayout).map(LayoutI17.dataLayout2MemoryLayout).toSeq*
        )
      case _ =>
        FunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.view.map(dm.toDataLayout).map(LayoutI17.dataLayout2MemoryLayout).toSeq*
        )

    Mem17(
      linker
        .upcallStub(mh, fd, scope)
        .nn
        .asSegment(LayoutI17.pointerLayout.size.toLong, scope)
        .nn
    )

  override def allocate(descriptor: TypeDescriptor, num: Int): Mem =
    Mem17(
      segmentAllocator
        .allocate(descriptor.size.toLong * num, descriptor.alignment.toLong)
        .nn
    )
  override def base: Object = segmentAllocator

package fr.hammons.slinc

import jdk.incubator.foreign.{
  SegmentAllocator,
  ResourceScope,
  CLinker,
  FunctionDescriptor as JFunctionDescriptor
}, CLinker.C_POINTER
import fr.hammons.slinc.modules.descriptorModule17

class Allocator17(
    segmentAllocator: SegmentAllocator,
    scope: ResourceScope,
    linker: CLinker
) extends Allocator:

  override def upcall[Fn](descriptor: FunctionDescriptor, target: Fn): Mem =
    val mh = methodHandleFromFn(descriptor, target)
    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        JFunctionDescriptor.of(
          descriptorModule17.toMemoryLayout(r),
          descriptor.inputDescriptors.map(descriptorModule17.toMemoryLayout)*
        )
      case _ =>
        JFunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.map(descriptorModule17.toMemoryLayout)*
        )

    Mem17(
      linker
        .upcallStub(mh, fd, scope)
        .nn
        .asSegment(C_POINTER.nn.byteSize(), scope)
        .nn
    )

  override def allocate(descriptor: TypeDescriptor, num: Int): Mem =
    Mem17(
      segmentAllocator
        .allocate(descriptor.size.toLong * num, descriptor.alignment.toLong)
        .nn
    )
  override def base: Object = segmentAllocator

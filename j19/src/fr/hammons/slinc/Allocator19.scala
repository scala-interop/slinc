package fr.hammons.slinc

import java.lang.foreign.SegmentAllocator
import java.lang.foreign.MemorySession
import java.lang.foreign.Linker
import java.lang.foreign.{FunctionDescriptor as JFunctionDescriptor}
import fr.hammons.slinc.modules.descriptorModule19

class Allocator19(
    segmentAllocator: SegmentAllocator,
    scope: MemorySession,
    linker: Linker
) extends Allocator:

  override def upcall[Fn](descriptor: FunctionDescriptor, target: Fn): Mem =
    val size = descriptor.inputDescriptors.size

    val mh = this.methodHandleFromFn(descriptor, target)

    val fd = descriptor.outputDescriptor match
      case Some(r) =>
        JFunctionDescriptor.of(
          descriptorModule19.toMemoryLayout(r),
          descriptor.inputDescriptors.view
            .map(descriptorModule19.toMemoryLayout)
            .toSeq*
        )
      case _ =>
        JFunctionDescriptor.ofVoid(
          descriptor.inputDescriptors.view
            .map(descriptorModule19.toMemoryLayout)
            .toSeq*
        )

    Mem19(
      linker.upcallStub(mh, fd, scope).nn
    )

  override def allocate(descriptor: TypeDescriptor, num: Int): Mem = Mem19(
    segmentAllocator
      .allocate(descriptor.size.toLong * num, descriptor.alignment.toLong)
      .nn
  )

  override def base: Object = segmentAllocator

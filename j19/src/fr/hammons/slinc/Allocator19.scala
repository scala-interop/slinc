package fr.hammons.slinc

import java.lang.foreign.SegmentAllocator
import java.lang.foreign.MemorySession
import java.lang.foreign.Linker
import java.lang.foreign.FunctionDescriptor
import fr.hammons.slinc.modules.DescriptorModule

class Allocator19(
    segmentAllocator: SegmentAllocator,
    scope: MemorySession,
    linker: Linker,
    layoutI: LayoutI
)(using DescriptorModule) extends Allocator(layoutI):
  import layoutI.*

  override def upcall[Fn](descriptor: Descriptor, target: Fn): Mem =
    val size = descriptor.inputLayouts.size

    val mh = this.methodHandleFromFn(descriptor, target)

    val fd = descriptor.outputLayout match
      case Some(r) =>
        FunctionDescriptor.of(
          LayoutI19.dataLayout2MemoryLayout(r),
          descriptor.inputLayouts.map(LayoutI19.dataLayout2MemoryLayout)*
        )
      case _ =>
        FunctionDescriptor.ofVoid(
          descriptor.inputLayouts.map(LayoutI19.dataLayout2MemoryLayout)*
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

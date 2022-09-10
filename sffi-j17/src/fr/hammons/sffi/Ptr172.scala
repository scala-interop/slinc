package fr.hammons.sffi

import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.CLinker

object Ptr172 extends PtrI[MemorySegment, SegmentAllocator, LayoutInfo172.LayoutInfo, StructInfo172.StructInfo, Deref172.Deref, Assign172.Assign, InTransition172.InTransition
]:
  def alloc[A](layoutInfo: LayoutInfo172.LayoutInfo[A], num: Long)(using s: SegmentAllocator): MemorySegment = s.allocate(layoutInfo.context).nn

  given ptrInfo: LayoutInfo172.LayoutInfo[Ptr] with 
    val context = CLinker.C_POINTER.nn
    val size = context.byteSize()

  given ptrInTransition[A]: InTransition172.InTransition[Ptr[A]] with 
    def to(a: Ptr[A]): SegmentAllocator ?=> Object = a.mem.address().nn.asInstanceOf[Object]

  extension (ptr: Ptr[Byte]) def asString: String = CLinker.toJavaString(ptr.mem).nn
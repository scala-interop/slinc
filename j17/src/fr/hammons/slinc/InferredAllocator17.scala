package fr.hammons.slinc

import java.lang.ref.Cleaner
import jdk.incubator.foreign.{
  ResourceScope,
  FunctionDescriptor as JFunctionDescriptor,
  CLinker,
  SegmentAllocator
}, CLinker.{C_POINTER, VaList}
import fr.hammons.slinc.modules.descriptorModule17
import scala.compiletime.asMatchable

class InferredAllocator17(scope: ResourceScope, linker: CLinker)
    extends Allocator:
  val allocated = collection.concurrent.TrieMap
    .empty[ResourceScope.Handle, ResourceScope.Handle]
  val allocator = SegmentAllocator.arenaAllocator(scope).nn

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
    val mem = Mem17(
      linker
        .upcallStub(mh, fd, scope)
        .nn
        .asSegment(C_POINTER.nn.byteSize(), scope)
        .nn
    )
    registerValue(mem)
    mem

  private def registerValue(mem: Any): Unit =
    val handle = scope.acquire().nn

    allocated.put(handle, handle)
    InferredAllocator17.cleaner.register(
      mem,
      () =>
        allocated.remove(handle)
        scope.release(handle)
        if allocated.isEmpty then scope.close()
    )

  override def allocate(descriptor: TypeDescriptor, num: Int): Mem =
    val mem = Mem17(
      allocator
        .allocate(
          descriptor.size.toLong * num,
          descriptor.alignment.toLong
        )
        .nn
    )
    registerValue(mem)
    mem

  override def makeVarArgs(vbuilder: VarArgsBuilder): VarArgs =
    val ret = VarArgs17(
      VaList
        .make(
          _b =>
            val builder = _b.nn
            vbuilder.vs.foreach(
              _.use[DescriptorOf](dO ?=>
                v =>
                  Allocator17.makeVarArg(builder, dO.descriptor, v.asMatchable)
              )
            )
          ,
          scope
        )
        .nn
    )
    registerValue(ret)
    ret

  override def base: Object = allocator

  override def addCloseAction(fn: () => Unit): Unit =
    InferredAllocator17.cleaner.register(this, () => fn())

object InferredAllocator17:
  private val cleaner = Cleaner.create().nn

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

class Allocator17(
    segmentAllocator: SegmentAllocator,
    scope: ResourceScope,
    linker: CLinker,
    layoutI: LayoutI
) extends Allocator:
  import layoutI.*

  type Id[A] = A
  override def upcall[Fn](descriptor: Descriptor, target: Fn): Mem =
    val size = descriptor.inputLayouts.size
    val mh = MethodHandles.lookup.nn
      .findVirtual(
        Class.forName(s"scala.Function$size"),
        "apply",
        MethodType.genericMethodType(size)
      )
      .nn
      .bindTo(target)
    val fd = descriptor.outputLayout match
      case Some(r) =>
        FunctionDescriptor.of(
          LayoutI17.dataLayout2MemoryLayout(r),
          descriptor.inputLayouts.map(LayoutI17.dataLayout2MemoryLayout)*
        )
      case _ =>
        FunctionDescriptor.ofVoid(
          descriptor.inputLayouts.map(LayoutI17.dataLayout2MemoryLayout)*
        )

    Mem17(
      linker
        .upcallStub(mh.nn.asType(descriptor.toMethodType), fd, scope)
        .nn
        .asSegment(LayoutI17.pointerLayout.size.toLong, scope)
        .nn
    )

  override def allocate(layout: DataLayout, num: Int): Mem =
    Mem17(
      segmentAllocator
        .allocate(layout.size.toLong * num, layout.alignment.toLong)
        .nn
    )
  override def base: Object = segmentAllocator

class Scope17(layoutI: LayoutI) extends ScopeI.PlatformSpecific(layoutI):
  val linker = CLinker.getInstance().nn
  def createGlobalScope: GlobalScope = new GlobalScope:
    def apply[A](fn: (Allocator) ?=> A): A =
      val rs = ResourceScope.globalScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker, layoutI)
      fn

  def createConfinedScope: ConfinedScope = new ConfinedScope:
    def apply[A](fn: Allocator ?=> A): A =
      val rs = ResourceScope.newConfinedScope().nn
      given Allocator =
        Allocator17(SegmentAllocator.arenaAllocator(rs).nn, rs, linker, layoutI)
      val res = fn
      rs.close()
      res

  def createTempScope: TempScope = new TempScope:
    def apply[A](fn: Allocator ?=> A): A =
      given Allocator = Allocator17(
        TempAllocator.localAllocator(),
        ResourceScope.globalScope().nn,
        linker,
        layoutI
      )
      val res = fn
      TempAllocator.reset()
      res

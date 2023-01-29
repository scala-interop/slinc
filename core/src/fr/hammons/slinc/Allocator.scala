package fr.hammons.slinc

import fr.hammons.slinc.ScopeI.PlatformSpecific
import java.lang.invoke.{MethodHandle, MethodType, MethodHandles}

trait Allocator(layoutI: LayoutI):
  import layoutI.*
  def allocate(descriptor: TypeDescriptor, num: Int): Mem
  def upcall[Fn](descriptor: Descriptor, target: Fn): Mem
  protected def methodHandleFromFn[Fn](
      descriptor: Descriptor,
      target: Fn
  ): MethodHandle =
    val size = descriptor.inputLayouts.size
    val mh = MethodHandles.lookup.nn
      .findVirtual(
        Class.forName(s"scala.Function$size"),
        "apply",
        MethodType.genericMethodType(size)
      )
      .nn
      .bindTo(target)
      .nn
      .asType(descriptor.toMethodType)
      .nn
    mh
  def base: Object

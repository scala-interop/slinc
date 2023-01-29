package fr.hammons.slinc

import fr.hammons.slinc.ScopeI.PlatformSpecific
import java.lang.invoke.{MethodHandle, MethodType, MethodHandles}
import fr.hammons.slinc.modules.DescriptorModule

trait Allocator:
  def allocate(descriptor: TypeDescriptor, num: Int): Mem
  def upcall[Fn](descriptor: FunctionDescriptor, target: Fn): Mem
  protected def methodHandleFromFn[Fn](
      descriptor: FunctionDescriptor,
      target: Fn
  )(using DescriptorModule): MethodHandle =
    val size = descriptor.inputDescriptors.size
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

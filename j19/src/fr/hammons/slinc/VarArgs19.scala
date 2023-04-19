package fr.hammons.slinc

import java.lang.foreign.VaList
import java.lang.foreign.ValueLayout
import fr.hammons.slinc.modules.{
  transitionModule19,
  descriptorModule19,
  LinkageModule19
}
import scala.util.chaining.*
import java.lang.foreign.MemorySegment

private class VarArgs19(vaList: VaList) extends VarArgs:

  override def ptr: Ptr[Nothing] = vaList
    .address()
    .nn
    .pipe(MemorySegment.ofAddress(_, 1, vaList.session()))
    .nn
    .pipe(Mem19.apply(_))
    .pipe(Ptr(_, Bytes(0)))

  private def skip(td: TypeDescriptor): Unit = td match
    case ByteDescriptor | ShortDescriptor | IntDescriptor =>
      vaList.skip(ValueLayout.JAVA_INT)
    case LongDescriptor => vaList.skip(ValueLayout.JAVA_LONG)
    case FloatDescriptor | DoubleDescriptor =>
      vaList.skip(ValueLayout.JAVA_DOUBLE)
    case PtrDescriptor | VaListDescriptor => vaList.skip(ValueLayout.ADDRESS)
    case sd: StructDescriptor =>
      vaList.skip(descriptorModule19.toGroupLayout(sd))
    case AliasDescriptor(real) => skip(real)

  override def skip[A](using dO: DescriptorOf[A]): Unit = skip(dO.descriptor)

  override def copy(): VarArgs = VarArgs19(vaList.copy().nn)

  def as(td: TypeDescriptor): Object =
    td match
      case ByteDescriptor =>
        Byte.box(vaList.nextVarg(ValueLayout.JAVA_INT).toByte)
      case ShortDescriptor =>
        Short.box(vaList.nextVarg(ValueLayout.JAVA_INT).toShort)
      case IntDescriptor =>
        Int.box(vaList.nextVarg(ValueLayout.JAVA_INT).toInt)
      case LongDescriptor =>
        Long.box(vaList.nextVarg(ValueLayout.JAVA_LONG).toLong)
      case FloatDescriptor =>
        Float.box(vaList.nextVarg(ValueLayout.JAVA_DOUBLE).toFloat)
      case DoubleDescriptor =>
        Double.box(vaList.nextVarg(ValueLayout.JAVA_DOUBLE).toDouble)
      case PtrDescriptor =>
        vaList.nextVarg(ValueLayout.ADDRESS).nn
      case sd: StructDescriptor =>
        LinkageModule19.tempScope(alloc ?=>
          vaList
            .nextVarg(
              descriptorModule19.toGroupLayout(sd),
              alloc.asInstanceOf[Allocator19].segmentAllocator
            )
            .nn
        )
      case AliasDescriptor(real) => as(real)
      case VaListDescriptor      => ???

  override def get[A](using d: DescriptorOf[A]): A =
    transitionModule19.methodReturn[A](d.descriptor, as(d.descriptor))
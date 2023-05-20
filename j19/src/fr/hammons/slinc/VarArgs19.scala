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
import java.lang.foreign.GroupLayout

private class VarArgs19(vaList: VaList) extends VarArgs:

  override def mem: Mem = vaList
    .address()
    .nn
    .pipe(MemorySegment.ofAddress(_, 1, vaList.session()))
    .nn
    .pipe(Mem19.apply(_))

  private def skip(td: TypeDescriptor): Unit = td match
    case ByteDescriptor | ShortDescriptor | IntDescriptor =>
      vaList.skip(ValueLayout.JAVA_INT)
    case LongDescriptor => vaList.skip(ValueLayout.JAVA_LONG)
    case FloatDescriptor | DoubleDescriptor =>
      vaList.skip(ValueLayout.JAVA_DOUBLE)
    case PtrDescriptor | VaListDescriptor | _: SetSizeArrayDescriptor =>
      vaList.skip(ValueLayout.ADDRESS)
    case sd: StructDescriptor =>
      vaList.skip(descriptorModule19.toGroupLayout(sd))
    case cd: CUnionDescriptor =>
      vaList.skip(descriptorModule19.toMemoryLayout(cd))
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
      case PtrDescriptor | VaListDescriptor | _: SetSizeArrayDescriptor =>
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
      case cud: CUnionDescriptor =>
        val desc =
          descriptorModule19.toMemoryLayout(cud).asInstanceOf[GroupLayout]

        LinkageModule19.tempScope(alloc ?=>
          vaList
            .nextVarg(
              descriptorModule19.toMemoryLayout(cud).asInstanceOf[GroupLayout],
              alloc.asInstanceOf[Allocator19].segmentAllocator
            )
            .nn
        )
  override def get[A](using d: DescriptorOf[A]): A =
    transitionModule19.methodReturn[A](d.descriptor, as(d.descriptor))

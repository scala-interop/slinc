package fr.hammons.slinc

import jdk.incubator.foreign.CLinker.VaList
import jdk.incubator.foreign.CLinker.{C_INT, C_LONG_LONG, C_DOUBLE, C_POINTER}
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.GroupLayout
import fr.hammons.slinc.modules.{
  LinkageModule17,
  descriptorModule17,
  transitionModule17
}

import scala.util.chaining.*

class VarArgs17(args: VaList) extends VarArgs:
  private def get(td: TypeDescriptor): Object =
    td match
      case ByteDescriptor   => Byte.box(args.vargAsInt(C_INT).toByte)
      case ShortDescriptor  => Short.box(args.vargAsInt(C_INT).toShort)
      case IntDescriptor    => Int.box(args.vargAsInt(C_INT))
      case LongDescriptor   => Long.box(args.vargAsLong(C_LONG_LONG))
      case FloatDescriptor  => Float.box(args.vargAsDouble(C_DOUBLE).toFloat)
      case DoubleDescriptor => Double.box(args.vargAsDouble(C_DOUBLE))
      case PtrDescriptor | _: SetSizeArrayDescriptor | VaListDescriptor =>
        args.vargAsAddress(C_POINTER).nn
      case sd: StructDescriptor =>
        LinkageModule17.tempScope(alloc ?=>
          args
            .vargAsSegment(
              descriptorModule17.toGroupLayout(sd),
              alloc.base.asInstanceOf[SegmentAllocator]
            )
            .nn
        )
      case AliasDescriptor(real) => get(real)
      case cud: CUnionDescriptor =>
        LinkageModule17.tempScope(alloc ?=>
          args
            .vargAsSegment(
              descriptorModule17.toMemoryLayout(cud).asInstanceOf[GroupLayout],
              alloc.base.asInstanceOf[SegmentAllocator]
            )
            .nn
        )
  def get[A](using d: DescriptorOf[A]): A =
    transitionModule17.methodReturn[A](d.descriptor, get(d.descriptor))

  private def skip(td: TypeDescriptor): Unit =
    td match
      case ByteDescriptor                            => args.skip(C_INT)
      case ShortDescriptor                           => args.skip(C_INT)
      case IntDescriptor                             => args.skip(C_INT)
      case LongDescriptor                            => args.skip(C_LONG_LONG)
      case FloatDescriptor                           => args.skip(C_DOUBLE)
      case DoubleDescriptor                          => args.skip(C_DOUBLE)
      case PtrDescriptor | _: SetSizeArrayDescriptor => args.skip(C_POINTER)
      case sd: StructDescriptor =>
        args.skip(descriptorModule17.toGroupLayout(sd))
      case AliasDescriptor(real) => skip(real)
      case VaListDescriptor      => args.skip(C_POINTER)
      case cud: CUnionDescriptor =>
        args.skip(descriptorModule17.toMemoryLayout(cud))

  def skip[A](using dO: DescriptorOf[A]): Unit = skip(dO.descriptor)

  def mem: Mem = args
    .address()
    .nn
    .asSegment(1, args.scope())
    .nn
    .pipe(Mem17(_))

  def copy(): VarArgs = args.copy().nn.pipe(VarArgs17(_))

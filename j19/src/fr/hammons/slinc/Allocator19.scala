package fr.hammons.slinc

import java.lang.foreign.SegmentAllocator
import java.lang.foreign.MemorySession
import java.lang.foreign.Linker
import java.lang.foreign.{FunctionDescriptor as JFunctionDescriptor}
import fr.hammons.slinc.modules.{descriptorModule19, transitionModule19}
import java.lang.foreign.VaList
import java.lang.foreign.ValueLayout
import scala.jdk.FunctionConverters.*
import java.lang.foreign.Addressable
import java.lang.foreign.MemorySegment
import java.lang.foreign.GroupLayout
import fr.hammons.slinc.modules.LinkageModule19

class Allocator19(
    val segmentAllocator: SegmentAllocator,
    scope: MemorySession,
    linker: Linker
) extends Allocator:

  override def upcall[Fn](descriptor: FunctionDescriptor, target: Fn): Mem =
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

  private def build(
      builder: VaList.Builder,
      typeDescriptor: TypeDescriptor,
      value: Matchable
  ): Unit =
    (typeDescriptor, value) match
      case (ByteDescriptor, v: Byte) =>
        builder.addVarg(ValueLayout.JAVA_INT, v.toInt)
      case (ShortDescriptor, v: Short) =>
        builder.addVarg(ValueLayout.JAVA_INT, v.toInt)
      case (IntDescriptor, v: Int) =>
        builder.addVarg(ValueLayout.JAVA_INT, v)
      case (LongDescriptor, v: Long) =>
        builder.addVarg(ValueLayout.JAVA_LONG, v)
      case (FloatDescriptor, v: Float) =>
        builder.addVarg(ValueLayout.JAVA_DOUBLE, v.toFloat)
      case (DoubleDescriptor, v: Double) =>
        builder.addVarg(ValueLayout.JAVA_DOUBLE, v)
      case (PtrDescriptor, v: Ptr[?]) =>
        builder.addVarg(
          ValueLayout.ADDRESS,
          LinkageModule19.tempScope(alloc ?=>
            transitionModule19
              .methodArgument[Ptr[?]](PtrDescriptor, v, alloc)
              .asInstanceOf[Addressable]
          )
        )
      case (sd: StructDescriptor, v) =>
        builder.addVarg(
          descriptorModule19.toGroupLayout(sd),
          LinkageModule19.tempScope(alloc ?=>
            transitionModule19
              .methodArgument(sd, v, alloc)
              .asInstanceOf[MemorySegment]
          )
        )
      case (AliasDescriptor(real), v) =>
        build(builder, real, v)
      case (VaListDescriptor, varArg: VarArgs19) =>
        builder.addVarg(
          ValueLayout.ADDRESS,
          LinkageModule19.tempScope(alloc ?=>
            transitionModule19
              .methodArgument[VarArgs](VaListDescriptor, varArg, alloc)
              .asInstanceOf[Addressable]
          )
        )
      case (cUnionDescriptor: CUnionDescriptor, c: CUnion[?]) =>
        builder.addVarg(
          descriptorModule19.toMemoryLayout(cUnionDescriptor) match
            case g: GroupLayout => g
            case _ =>
              throw Error("CUnionDescriptor didn't resolve to group layout??")
          ,
          c.mem.asBase match
            case ms: MemorySegment => ms
            case _                 => throw Error("Illegal datatype")
        )
      case (td, d) =>
        throw Error(s"Unsupported datatype for $td - $d")

  override def makeVarArgs(vbuilder: VarArgsBuilder): VarArgs =
    import scala.compiletime.asMatchable

    val vaList =
      VaList.make(
        _b =>
          val builder = _b.nn
          vbuilder.vs.foreach: variadic =>
            variadic.use[DescriptorOf](dO ?=>
              v => build(builder, dO.descriptor, v.asMatchable)
            )
        ,
        scope
      )

    new VarArgs19(vaList.nn)

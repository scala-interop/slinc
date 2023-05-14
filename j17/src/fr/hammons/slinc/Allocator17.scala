package fr.hammons.slinc

import jdk.incubator.foreign.{
  SegmentAllocator,
  Addressable,
  MemorySegment,
  ResourceScope,
  CLinker,
  FunctionDescriptor as JFunctionDescriptor,
  GroupLayout
}, CLinker.{C_POINTER, C_INT, C_LONG_LONG, C_DOUBLE, VaList}
import fr.hammons.slinc.modules.{descriptorModule17, transitionModule17}
import fr.hammons.slinc.modules.LinkageModule17

class Allocator17(
    segmentAllocator: SegmentAllocator,
    scope: ResourceScope,
    linker: CLinker
) extends Allocator:
  import scala.compiletime.asMatchable
  override def addCloseAction(fn: () => Unit): Unit =
    scope.addCloseAction(
      new Runnable:
        def run() = fn()
    )

  override def makeVarArgs(vbuilder: VarArgsBuilder): VarArgs =
    VarArgs17(
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

    Mem17(
      linker
        .upcallStub(mh, fd, scope)
        .nn
        .asSegment(C_POINTER.nn.byteSize(), scope)
        .nn
    )

  override def allocate(descriptor: TypeDescriptor, num: Int): Mem =
    Mem17(
      segmentAllocator
        .allocate(descriptor.size.toLong * num, descriptor.alignment.toLong)
        .nn
    )
  override def base: Object = segmentAllocator

object Allocator17:
  def makeVarArg(
      builder: VaList.Builder,
      td: TypeDescriptor,
      v: Matchable
  ): Unit = (td, v) match
    case (ByteDescriptor, v: Byte)   => builder.vargFromInt(C_INT, v.toInt)
    case (ShortDescriptor, v: Short) => builder.vargFromInt(C_INT, v.toInt)
    case (IntDescriptor, v: Int)     => builder.vargFromInt(C_INT, v)
    case (LongDescriptor, v: Long)   => builder.vargFromLong(C_LONG_LONG, v)
    case (FloatDescriptor, v: Float) =>
      builder.vargFromDouble(C_DOUBLE, v.toFloat)
    case (DoubleDescriptor, v: Double) => builder.vargFromDouble(C_DOUBLE, v)
    case (PtrDescriptor, v: Ptr[?]) =>
      LinkageModule17.tempScope(alloc ?=>
        builder.vargFromAddress(
          C_POINTER,
          transitionModule17
            .methodArgument(PtrDescriptor, v, alloc)
            .asInstanceOf[Addressable]
        )
      )
    case (sd: StructDescriptor, v) =>
      LinkageModule17.tempScope(alloc ?=>
        builder.vargFromSegment(
          descriptorModule17.toGroupLayout(sd),
          transitionModule17
            .methodArgument(sd, v, alloc)
            .asInstanceOf[MemorySegment]
        )
      )
    case (AliasDescriptor(real), _) => makeVarArg(builder, real, v)
    case (VaListDescriptor, v: VarArgs17) =>
      LinkageModule17.tempScope(alloc ?=>
        builder.vargFromAddress(
          C_POINTER,
          transitionModule17
            .methodArgument(VaListDescriptor, v, alloc)
            .asInstanceOf[Addressable]
        )
      )
    case (cd: CUnionDescriptor, v: CUnion[?]) =>
      builder.vargFromSegment(
        descriptorModule17.toMemoryLayout(cd) match
          case gl: GroupLayout => gl
          case _ => throw Error("got a non group layout from CUnionDescriptor")
        ,
        v.mem.asBase match
          case ms: MemorySegment => ms
          case _ => throw Error("base of mem was not J17 MemorySegment!!")
      )
    case (a, d) =>
      throw Error(
        s"Unsupported type descriptor/data pairing for VarArgs: $a - $d"
      )

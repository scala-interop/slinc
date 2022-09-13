package fr.hammons.sffi

import jdk.incubator.foreign.{
  ResourceScope,
  SegmentAllocator,
  CLinker,
  MemoryAccess,
  MemoryAddress,
  MemorySegment,
  MemoryLayout,
  GroupLayout,
  FunctionDescriptor,
  SymbolLookup
}, MemoryLayout.PathElement
import java.lang.invoke.{MethodType, MethodHandle}
import scala.compiletime.erasedValue
import scala.annotation.targetName
import java.beans.MethodDescriptor

@JavaPlatform("17")
object FFI17 extends FFI, LayoutInfo17, Basics17:

  protected def offsetOf(name: String, context: MemoryLayout): Long =
    context match
      case g: GroupLayout => g.byteOffset(PathElement.groupElement(name))
      case _              => ???

  protected def sizeOf(context: MemoryLayout): Long = context.byteSize()

  @targetName("stringToNative")
  override def toNative[B <: Tuple](string: String)(using
      allocation: Allocatable[Byte]
  )(using Scope, Allocator, PtrShape[Byte, B]): Ptr[Byte] =
    Ptr(CLinker.toCString(string, summon[SegmentAllocator]).nn, 0)

  type Allocator = SegmentAllocator

  override given intOutTransition: OutTransition[Int] with
    def from(obj: Object) = obj.asInstanceOf[Int]


  override given floatOutTransition: OutTransition[Float] = ???

  given ptrInTransition[A]: InTransition[Pointer[A]] with
    def to(a: Ptr[A]): (Allocator) ?=> Object =
      a.mem.address().nn.asInstanceOf[Object]

  given ptrOutTransition[A](using l: LayoutInfo[A]): OutTransition[Pointer[A]]
    with
    def from(obj: Object): Pointer[A] =
      Ptr(
        obj
          .asInstanceOf[MemoryAddress]
          .asSegment(l.size, ResourceScope.globalScope())
          .nn,
        0
      )

  given byteInTransition: InTransition[Byte] = ???

  // override given ptrOutTransition[A]: OutTransition[Pointer[A]] = ???

  private val linker = CLinker.getInstance().nn

  def methodGen(
      name: String,
      inputs: Seq[MemoryLayout],
      output: Option[MemoryLayout],
      addendum: Seq[MemoryLayout]
  ): MethodHandle =
    val totalInputs = inputs ++ addendum
    val fd = output match
      case Some(outLayout) => FunctionDescriptor.of(outLayout, totalInputs*).nn
      case None            => FunctionDescriptor.ofVoid(totalInputs*).nn

    val md = (output, totalInputs.headOption) match
      case (Some(outLayout), Some(in)) =>
        MethodType
          .methodType(
            carrierFromContext(outLayout),
            carrierFromContext(in),
            totalInputs.tail.map(carrierFromContext)*
          )
          .nn
      case (Some(outLayout), None) =>
        MethodType.methodType(carrierFromContext(outLayout)).nn
      case (None, Some(in)) =>
        VoidHelper
          .methodTypeV(
            carrierFromContext(in),
            totalInputs.tail.map(carrierFromContext)*
          )
          .nn
      case (None, None) => VoidHelper.methodTypeV().nn

    linker
      .downcallHandle(CLinker.systemLookup().nn.lookup(name).nn.get(), md, fd)
      .nn


  override given longOutTransition: OutTransition[Long] = ???

  override protected def localAllocator(): Allocator =
    TempAllocator.localAllocator()

  given sa[A, Size <: Int](using
      value: ValueOf[Size],
      layout: LayoutInfo[A]
  ): LayoutInfo[StaticArray[A, Size]] with
    val context = MemoryLayout.sequenceLayout(value.value, layout.context).nn
    val size = context.byteSize()

  override protected def resetAllocator(): Unit = TempAllocator.reset()

  override given intInTransition: InTransition[Int] with
    def to(i: Int) = toNativeCompat(i.asInstanceOf[Object])

  override given floatInTransition: InTransition[Float] with
    def to(i: Float) = toNativeCompat(i.asInstanceOf[Object])

  given longInTransition: InTransition[Long] with
    def to(i: Long) = toNativeCompat(i.asInstanceOf[Object])

  type RawMem = MemorySegment

  override given intDeref: Deref[Int] with
    def deref(b: RawMem, offset: Long): Int =
      MemoryAccess.getIntAtOffset(b, offset).nn

    def toArray(b: RawMem, index: Long, size: Long): Array[Int] =
      val arr = Array.ofDim[Int](size.toInt)

      MemorySegment.ofArray(arr).nn.copyFrom(b)

      arr
  override given intAssign: Assign[Int] with
    def assign(b: RawMem, offset: Long, a: Int): Unit =
      MemoryAccess.setIntAtOffset(b, offset, a).nn

  given byteDeref: Deref[Byte] with
    def deref(b: RawMem, offset: Long): Byte =
      MemoryAccess.getByteAtOffset(b, offset).nn

    def toArray(b: RawMem, index: Long, size: Long): Array[Byte] =
      val arr = Array.ofDim[Byte](size.toInt)

      // println(size)
      val nb = b.address().nn.asSegment(size, b.scope()).nn
      MemorySegment.ofArray(arr).nn.copyFrom(nb.asSlice(0, size))

      arr

  // given fnHasKernel(using alloc: => Allocatable[() => Int, Scope17]): HasKernel[() => Int] with
  //   val kernel: PtrKernel[() => Int] = PtrKernel[() => Int,EmptyTuple]()

  override val Scope: ScopeSingleton[Scope, Allocator] =
    new ScopeSingleton[Scope, Allocator]:
      def apply[T](
          allocatorType: AllocatorType,
          shared: Boolean,
          global: Boolean
      )(code: Scope ?=> Allocator ?=> T): T =
        val resourceScope =
          if global then ResourceScope.globalScope().nn
          else if shared then ResourceScope.newSharedScope().nn
          else ResourceScope.newConfinedScope().nn

        given segmentAllocator: SegmentAllocator = allocatorType match
          case AllocatorType.Arena =>
            SegmentAllocator.arenaAllocator(resourceScope).nn
          case AllocatorType.Implicit =>
            SegmentAllocator.ofScope(resourceScope).nn

        given Scope = resourceScope

        val res = code

        if !global then resourceScope.close()
        res

  //class Scope17(val underlying: ResourceScope, val allocator: SegmentAllocator)

  override given intIsAllocatable: Allocatable[Int] with
    def apply(a: Int)(using ev: Scope, s: Allocator): Ptr[Int] =
      val r: RawMem = s.allocate(CLinker.C_INT).nn

      MemoryAccess.setInt(r, a).nn

      Ptr(r, 0)

    @targetName("arrayApply")
    def apply(a: Array[Int])(using ev: Scope, s: Allocator): Ptr[Int] =
      val r: RawMem = s.allocateArray(CLinker.C_INT, a.size).nn

      r.copyFrom(MemorySegment.ofArray(a).nn)

      Ptr(r, 0)
  end intIsAllocatable

  given byteIsAllocatable: Allocatable[Byte] with
    def apply(a: Byte)(using 
      ev: Scope,
        s: Allocator
    ): Pointer[Byte] =
      val r: RawMem = s.allocate(CLinker.C_CHAR).nn

      MemoryAccess.setByte(r, a)

      Ptr(r, 0)

    @targetName("arrayApply")
    def apply(a: Array[Byte])(using
      ev: Scope,
        s: Allocator
    ): Ptr[Byte] =
      val r: RawMem =
        s.allocateArray(CLinker.C_CHAR, a).nn

      Ptr(r, 0)
  end byteIsAllocatable

  type Context = MemoryLayout

  protected def groupContext(
      contextPieces: List[(String, MemoryLayout)]
  ): Context =
    MemoryLayout
      .structLayout(
        contextPieces.map((name, context) => context.withName(name))*
      )
      .nn

  protected def offsetOf(
      name: String,
      context: (MemoryLayout, Class[?])
  ): Long = context._1 match
    case gl: GroupLayout => gl.byteOffset(PathElement.groupElement(name).nn)
    case _               => ???

  protected def sizeOf(context: (MemoryLayout, Class[?])): Long =
    context._1.byteSize()

  // type ShapeContext[Shape] = _ShapeContext[Shape, Context]

  given IntContext: LayoutInfo[Int] with
    val context: Context = CLinker.C_INT.nn
    val size = CLinker.C_INT.nn.byteSize()

  given FloatContext: LayoutInfo[Float] with
    val context = CLinker.C_FLOAT.nn
    val size = CLinker.C_FLOAT.nn.byteSize()

  given LongContext: LayoutInfo[Long] with
    val context = CLinker.C_LONG.nn
    val size = CLinker.C_LONG.nn.byteSize()

  given ptrContext[A]: LayoutInfo[Ptr[A]] with
    val context = CLinker.C_POINTER.nn
    val size = CLinker.C_POINTER.nn.byteSize()

  given byteContext: LayoutInfo[Byte] with
    val context = CLinker.C_CHAR.nn
    val size = CLinker.C_CHAR.nn.byteSize()

  protected def alloc(layoutInfo: LayoutInfo[? <: AnyKind], size: Long)(using
      a: Allocator
  ): RawMem =
    a.allocateArray(layoutInfo.context, size).nn

  type Lookup = SymbolLookup
  def loaderLookup: Lookup = SymbolLookup.loaderLookup().nn
  type Symbol = MemoryAddress

  extension (l: Lookup)
    def lookup(methodName: String) = l.lookup(methodName).nn.orElseThrow().nn

  extension (p: Ptr[Byte]) def asString = CLinker.toJavaString(p.mem).nn

  def upcallGen(
      mh: MethodHandle,
      inputs: Seq[MemoryLayout],
      ret: Option[MemoryLayout]
  )(using Scope): RawMem =
    val fd = ret match
      case None      => FunctionDescriptor.ofVoid(inputs*)
      case Some(ret) => FunctionDescriptor.of(ret, inputs*)
    CLinker
      .getInstance()
      .nn
      .upcallStub(mh, fd, summon[Scope])
      .nn
      .asSegment(CLinker.C_POINTER.nn.byteSize(), summon[Scope])
      .nn

// inline given tupleContext[Shape <: Tuple]: ShapeContext[Shape] =
//   new _ShapeContext[Shape, FunctionDescriptor]:
//     val context: Context =
//       inline erasedValue[Shape] match
//         case _: *:[head,tail] =>
//           FunctionDescriptor.of(CLinker.C_INT, CLinker.C_INT).nn

//Ptr[Int](PtrKernel(() => ))

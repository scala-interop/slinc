package fr.hammons.sffi

import scala.compiletime.{erasedValue, constValue, summonInline, codeOf}
import scala.deriving.Mirror
import scala.annotation.targetName

trait WStruct:
  self: WBasics & WStructInfo & WAllocatable & WLayoutInfo & WDeref & WAssign & WInTransition & WOutTransition & WPtr =>

  protected def groupContext(contextPieces: List[(String, Context)]): Context
  protected def offsetOf(name: String, context: Context): Long
  protected def sizeOf(context: Context): Long


  trait Struct[P <: Product]
    extends StructInfo[P],
      Allocatable[P],
      Deref[P],
      Assign[P],
      InTransition[P], 
      OutTransition[P]

  object Struct:
    private inline def genContext[T <: Tuple]: List[(String, Context)] =
      inline erasedValue[T] match
        case _: ((name, t) *: rest) =>
          (
            constValue[name].toString,
            summonInline[LayoutInfo[t]].context
          ) :: genContext[rest]
        case _: EmptyTuple => List.empty[(String, Context)]

    private inline def genOffsets[T <: Tuple](context: Context): List[Long] =
      inline erasedValue[T] match
        case _: (name *: rest) =>
          offsetOf(constValue[name].toString(), context) :: genOffsets[rest](
            context
          )
        case EmptyTuple => Nil

    private inline def writeGen[T <: Tuple](
        mem: RawMem,
        place: Int,
        values: T,
        offsets: IArray[Long]
    ): Unit =
      inline erasedValue[T] match
        case _: (h *: t) =>
          inline values match
            case tup: (`h` *: `t`) =>
              summonInline[Assign[h]].assign(mem, offsets(place), tup.head)
              writeGen[t](mem, place + 1, tup.tail, offsets)
        case _: EmptyTuple => ()

    private inline def subassign[V <: Tuple](b: RawMem, offset: Long, a: V, offsets: IArray[Long]): Unit = 
      writeGen[V](b, 0, a, offsets)

    private inline def readGenTup[T <: Tuple](
        mem: RawMem,
        offset: Long,
        place: Int,
        offsets: IArray[Long],
        output: Array[Any]
    ): Unit =
      inline erasedValue[T] match
        case _: (h *: t) =>
          output.update(place,
            summonInline[Deref[h]].deref(
              mem,
              offset + offsets(place)
            )
          )
          readGenTup[t](mem, offset, place + 1, offsets, output)
        case _: EmptyTuple =>
          ()

    // private inline def readGen[P <: Product](using
    //     m: Mirror.ProductOf[P]
    // )(mem: RawMem, offsets: IArray[Long]): P =
    //   val arr = Array.ofDim[Any](offsets.length)
    //   readGenTup[m.MirroredElemTypes](mem, 0, offsets, arr)
    //   m.fromProduct(Tuple.fromArray(arr))

    inline def derived[P <: Product, T <: Tuple, V <: Tuple](using
        _m: Mirror.ProductOf[P],
        ev: T =:= Tuple.Zip[_m.MirroredElemLabels, _m.MirroredElemTypes],
        ev2: _m.MirroredElemTypes =:= V
    ) = new Struct[P]:
      val context = groupContext(
        genContext[T].asInstanceOf[List[(String, Context)]]
      )
      val offsets = IArray.unsafeFromArray(
        genOffsets[_m.MirroredElemLabels](context).toArray
      )
      val size = sizeOf(context)

      override def apply(p: P)(using s: Scope, a: Allocator): Ptr[P] =
        val mem = alloc(this, 1)
        assign(mem, 0, p)
        Ptr(mem,0)

      @targetName("arrayApply")
      override def apply(p: Array[P])(using Scope, Allocator): Ptr[P] = ???

      //var printCode = true
      def assign(b: RawMem, offset: Long, a: P) =
        //subassign[V](b, offset, Tuple.fromProductTyped(a), offsets)
        writeGen[V](b, 0, Tuple.fromProductTyped(a), offsets)
        

      private val arr = Array.ofDim[Any](offsets.length)
      def deref(b: RawMem, offset: Long) =
        readGenTup[Values[T]](b, offset, 0, offsets, arr)
        _m.fromProduct(Tuple.fromArray(arr))

      def toArray(b: RawMem, offset: Long, size: Long) = ???

      def from(o: Object) = deref(o.asInstanceOf[RawMem], 0)
      def to(a: P): Allocator ?=> Object = 
        val mem = alloc(this, 1)
        assign(mem, 0, a)
        mem.asInstanceOf[Object]
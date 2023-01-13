package fr.hammons.slinc

import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.deriving.Mirror.ProductOf
import scala.reflect.ClassTag
import scala.quoted.*
import java.lang.invoke.MethodType
import scala.annotation.varargs
import container.*

trait LayoutOf[A <: AnyKind]:
  val layout: DataLayout

object LayoutOf:
  given [A](using
      c: ContextProof[LayoutOf *::: End, A]
  ): LayoutOf[A] = c.tup.head

trait LayoutOfStruct[A <: Product] extends LayoutOf[A]:
  val layout: DataLayout

class LayoutI(platformSpecific: LayoutI.PlatformSpecific):
  given LayoutOf[Char] with
    val layout = platformSpecific.shortLayout

  given LayoutOf[Int] with
    val layout = platformSpecific.intLayout

  given LayoutOf[Long] with
    val layout = platformSpecific.longLayout

  given LayoutOf[Float] with
    val layout = platformSpecific.floatLayout

  given LayoutOf[Short] with
    val layout = platformSpecific.shortLayout

  given LayoutOf[Byte] with
    val layout = platformSpecific.byteLayout

  given LayoutOf[Double] with
    val layout = platformSpecific.doubleLayout

  given ptrGen: LayoutOf[Ptr] with
    val layout = platformSpecific.pointerLayout

  given fnLayoutGen[A](using Fn[A, ?, ?]): LayoutOf[A] with
    val layout = platformSpecific.pointerLayout

  extension (d: Descriptor)
    def toMethodType: MethodType =
      import platformSpecific.toCarrierType
      d match
        case Descriptor(head +: tail, vargs, None) =>
          VoidHelper
            .methodTypeV(
              toCarrierType(head),
              tail.concat(vargs).map(toCarrierType)*
            )
            .nn
        case Descriptor(head +: tail, vargs, Some(r)) =>
          MethodType
            .methodType(
              toCarrierType(r),
              toCarrierType(head),
              tail.concat(vargs).map(toCarrierType)*
            )
            .nn
        case Descriptor(_, _, None) => VoidHelper.methodTypeV().nn
        case Descriptor(_, _, Some(r)) =>
          MethodType.methodType(toCarrierType(r)).nn

  given [A]: LayoutOf[Ptr[A]] = ptrGen.asInstanceOf[LayoutOf[Ptr[A]]]

  inline def structLayout[P <: Product](using
      m: ProductOf[P],
      ct: ClassTag[P]
  ) =
    getStructLayout[P](
      structLayoutHelper[Tuple.Zip[m.MirroredElemLabels, m.MirroredElemTypes]]*
    )

  def genLayoutList(remainingLayouts: Seq[DataLayout], alignment: Long, currentLocation: Long, finishedLayout: Seq[DataLayout]): Seq[DataLayout] = 
    remainingLayouts match 
      case head :: next => 
        val headAlignment = head.alignment.toLong
        val misalignment = currentLocation % headAlignment
        val toAdd = if misalignment == 0 then 
          Seq(head)
        else 
          val paddingNeeded = headAlignment - misalignment

          Seq(PaddingLayout(Bytes(paddingNeeded), head.byteOrder), head)
        genLayoutList(next, alignment, currentLocation + toAdd.view.map(_.size.toLong).sum, finishedLayout ++ toAdd)
      case _ => 
        val misalignment = (currentLocation % alignment)
        finishedLayout ++
          (if misalignment != 0 then 
            Seq(PaddingLayout(Bytes(alignment - misalignment), ByteOrder.HostDefault))
          else 
            Seq.empty
          )

  def getStructLayout[T](
      layouts: DataLayout*
  )(using po: ProductOf[T], ct: ClassTag[T]): StructLayout =
    val alignment = layouts.view.map(_.alignment.toLong).max


    val generatedLayout = genLayoutList(layouts, alignment, 0, Seq.empty)

    val members = generatedLayout match 
      case head :: next => next.foldLeft(Vector(StructMember(head, head.name, Bytes(0))) -> head.size) {
        case ((seq, currentOffset), layout) => 
          (seq :+ StructMember(layout, layout.name, currentOffset)) -> (currentOffset + layout.size)
      }._1
      case _ => Vector.empty

    StructLayout(
      None,
      Bytes(members.view.map(_.layout.size.toLong).sum),
      Bytes(members.view.map(_.layout.alignment.toLong).max),
      ByteOrder.HostDefault,
      po.fromProduct(_).asInstanceOf[Product],
      ct.runtimeClass,
      members
    )
  

  private inline def structLayoutHelper[T <: Tuple]: List[DataLayout] =
    inline erasedValue[T] match
      case _: ((name, value) *: t) =>
        summonInline[LayoutOf[value]].layout
          .withName(constValue[name].toString) :: structLayoutHelper[t]
      case _: EmptyTuple => Nil

object LayoutI:
  trait PlatformSpecific:
    val intLayout: IntLayout
    val longLayout: LongLayout
    val floatLayout: FloatLayout
    val shortLayout: ShortLayout
    val doubleLayout: DoubleLayout
    val pointerLayout: PointerLayout
    val byteLayout: ByteLayout
    def toCarrierType(dataLayout: DataLayout): Class[?]

  def getLayoutFor[A](using Quotes, Type[A]) =
    import quotes.reflect.*
    val expr = Expr
      .summon[LayoutOf[A]]
      .getOrElse(
        report.errorAndAbort(s"Cannot find a layout of ${Type.show[A]}")
      )

    '{ $expr.layout }

  inline def tupLayouts[A <: Tuple]: List[DataLayout] =
    inline erasedValue[A] match
      case _: (head *: tail) =>
        summonInline[LayoutOf[head]].layout :: tupLayouts[tail]
      case _ => Nil

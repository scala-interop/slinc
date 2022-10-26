package fr.hammons.slinc

import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.deriving.Mirror
import scala.reflect.ClassTag
import scala.quoted.*
import java.lang.invoke.MethodType
import scala.annotation.varargs

trait LayoutOf[A <: AnyKind]:
  val layout: DataLayout

trait LayoutOfStruct[A <: Product] extends LayoutOf[A]:
  val layout: DataLayout

class LayoutI(platformSpecific: LayoutI.PlatformSpecific):
  import platformSpecific.getStructLayout
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
      m: Mirror.ProductOf[P],
      ct: ClassTag[P]
  ) =
    getStructLayout[P](
      structLayoutHelper[Tuple.Zip[m.MirroredElemLabels, m.MirroredElemTypes]]*
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
    def getStructLayout[T](
        layouts: DataLayout*
    )(using Mirror.ProductOf[T], scala.reflect.ClassTag[T]): StructLayout
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

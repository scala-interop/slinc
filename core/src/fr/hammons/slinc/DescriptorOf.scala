package fr.hammons.slinc

import fr.hammons.slinc.container.*
import scala.quoted.*
import scala.compiletime.{summonInline, erasedValue}
import scala.NonEmptyTuple

/** Typeclass that summons TypeDescriptors
  */
trait DescriptorOf[A] extends MethodCompatible[A]:
  val descriptor: TypeDescriptor { type Inner = A }

object DescriptorOf:
  /** Convenience method for summoning the TypeDescriptor attached to
    * DescriptorOf.
    *
    * @example
    *   DescriptorOf[Int] //returns IntDescriptor
    *
    * @param d
    * @return
    *   TypeDescriptor for type A
    */
  def apply[A](using d: DescriptorOf[A]) = d.descriptor

  // compatibility with [[ContextProof]]
  given [A](using
      c: ContextProof[DescriptorOf *::: End, A]
  ): DescriptorOf[A] = c.tup.head

  given DescriptorOf[Byte] with
    val descriptor: TypeDescriptor { type Inner = Byte } = ByteDescriptor

  given DescriptorOf[Short] with
    val descriptor: TypeDescriptor { type Inner = Short } = ShortDescriptor

  given DescriptorOf[Int] with
    val descriptor: TypeDescriptor { type Inner = Int } = IntDescriptor

  given DescriptorOf[Long] with
    val descriptor: TypeDescriptor { type Inner = Long } = LongDescriptor

  given DescriptorOf[Float] with
    val descriptor: TypeDescriptor { type Inner = Float } = FloatDescriptor

  given DescriptorOf[Double] with
    val descriptor: TypeDescriptor { type Inner = Double } =
      DoubleDescriptor

  // this is the general DescriptorOf for all [[Ptr[A]]]
  private val ptrDescriptor = new DescriptorOf[Ptr[?]]:
    val descriptor: TypeDescriptor { type Inner = Ptr[?] } = PtrDescriptor

  given [A]: DescriptorOf[Ptr[A]] =
    ptrDescriptor.asInstanceOf[DescriptorOf[Ptr[A]]]

  given DescriptorOf[VarArgs] with
    val descriptor: TypeDescriptor { type Inner = VarArgs } = VaListDescriptor

  def getDescriptorFor[A](using Quotes, Type[A]) =
    import quotes.reflect.*
    val expr = Expr
      .summon[DescriptorOf[A]]
      .getOrElse(
        report.errorAndAbort(s"Cannot find a descriptor of ${Type.show[A]}")
      )

    '{ $expr.descriptor }

  private inline def helper[B <: Tuple]: Set[TypeDescriptor] =
    inline erasedValue[B] match
      case _: (a *: t)   => helper[t] + summonInline[DescriptorOf[a]].descriptor
      case _: EmptyTuple => Set.empty[TypeDescriptor]

  inline given [A <: NonEmptyTuple]: DescriptorOf[CUnion[A]] =
    new DescriptorOf[CUnion[A]]:
      val descriptor: CUnionDescriptor { type Inner = CUnion[? <: Tuple] } =
        CUnionDescriptor(helper[A])

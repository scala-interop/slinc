package fr.hammons.slinc

import fr.hammons.slinc.container.*
import scala.quoted.*

/** Typeclass that summons TypeDescriptors
  * 
  */
trait DescriptorOf[A]:
  val descriptor: TypeDescriptor

object DescriptorOf:
  /** Convenience method for summoning the TypeDescriptor attached to DescriptorOf.
    * 
    * @example DescriptorOf[Int] //returns IntDescriptor
    *
    * @param d
    * @return TypeDescriptor for type A
    */
  def apply[A](using d: DescriptorOf[A]) = d.descriptor

  // compatibility with [[ContextProof]]
  given [A](using
      c: ContextProof[DescriptorOf *::: End, A]
  ): DescriptorOf[A] = c.tup.head

  given DescriptorOf[Byte] with
    val descriptor: TypeDescriptor = ByteDescriptor

  given DescriptorOf[Short] with
    val descriptor: TypeDescriptor = ShortDescriptor

  given DescriptorOf[Int] with
    val descriptor: TypeDescriptor = IntDescriptor

  given DescriptorOf[Long] with
    val descriptor: TypeDescriptor = LongDescriptor

  given DescriptorOf[Float] with
    val descriptor: TypeDescriptor = FloatDescriptor

  given DescriptorOf[Double] with
    val descriptor: TypeDescriptor = DoubleDescriptor

  //this is the general DescriptorOf for all [[Ptr[A]]]
  private val ptrDescriptor = new DescriptorOf[Ptr[?]]:
    val descriptor: TypeDescriptor = PtrDescriptor

  given [A]: DescriptorOf[Ptr[A]] =
    ptrDescriptor.asInstanceOf[DescriptorOf[Ptr[A]]]

  def getDescriptorFor[A](using Quotes, Type[A]) =
    import quotes.reflect.*
    val expr = Expr.summon[DescriptorOf[A]].getOrElse(
      report.errorAndAbort(s"Cannot find a descriptor of ${Type.show[A]}")
    )

    '{$expr.descriptor}
package fr.hammons.slinc

import fr.hammons.slinc.container.*
import scala.quoted.*
import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.NonEmptyTuple
import scala.reflect.ClassTag
import fr.hammons.slinc.descriptors.Writer
import fr.hammons.slinc.jitc.OptimizableFn
import fr.hammons.slinc.descriptors.WriterContext
import fr.hammons.slinc.modules.MemWriter

/** Typeclass that summons TypeDescriptors
  */
trait DescriptorOf[A](val descriptor: TypeDescriptor { type Inner = A })(using
    ClassTag[A]
) extends MethodCompatible[A]:
  // val descriptor: TypeDescriptor { type Inner = A }
  val writer: Writer[A] = OptimizableFn((writerContext: WriterContext) ?=>
    _ {
      val expr = writerContext.rwm.writeExpr(descriptor)
      println(s"jitc: ${expr.show}")
      expr
    }.asInstanceOf[MemWriter[A]]
  )(instrumentation =>
    instrumentation((mem: Mem, offset: Bytes, value: A) =>
      instrumentation.instrument(
        descriptor.writer(using
          summon[WriterContext].rwm,
          summon[WriterContext].dm
        )(mem, offset, value)
      )
    )
  )

  val arrayWriter: Writer[Array[A]] = OptimizableFn(
    _(
      summon[WriterContext].rwm.writeArrayExpr(descriptor)
    ).asInstanceOf[MemWriter[Array[A]]]
  ) { instrumentation =>
    instrumentation {
      val size = descriptor.size(using summon[WriterContext].dm)
      (mem: Mem, offset: Bytes, value: Array[A]) =>
        instrumentation.instrument {
          var i = 0
          while i < value.length do
            writer.get(mem, size * i + offset, value(i))
            i += 1
        }
    }
  }

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

  given DescriptorOf[Byte](ByteDescriptor) with {}
  given DescriptorOf[Short](ShortDescriptor) with {}

  given DescriptorOf[Int](IntDescriptor) with {}

  given DescriptorOf[Long](LongDescriptor) with {}

  given DescriptorOf[Float](FloatDescriptor) with {}

  given DescriptorOf[Double](DoubleDescriptor) with {}

  // this is the general DescriptorOf for all [[Ptr[A]]]
  private val ptrDescriptor = new DescriptorOf[Ptr[?]](PtrDescriptor) {}

  given [A]: DescriptorOf[Ptr[A]] =
    ptrDescriptor.asInstanceOf[DescriptorOf[Ptr[A]]]

  given DescriptorOf[VarArgs](VaListDescriptor)

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
    new DescriptorOf[CUnion[A]](
      CUnionDescriptor(helper[A])
        .asInstanceOf[CUnionDescriptor { type Inner = CUnion[A] }]
    ) {}

  inline given [A, B <: Int](using innerDesc: DescriptorOf[A])(using
      classTag: ClassTag[innerDesc.descriptor.Inner]
  ): DescriptorOf[SetSizeArray[A, B]] = new DescriptorOf[SetSizeArray[A, B]](
    SetSizeArrayDescriptor(innerDesc.descriptor, constValue[B]).asInstanceOf[
      SetSizeArrayDescriptor { type Inner = SetSizeArray[A, B] }
    ]
  ) {}

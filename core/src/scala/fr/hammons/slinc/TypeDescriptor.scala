package fr.hammons.slinc

import modules.DescriptorModule
import fr.hammons.slinc.modules.{
  ReadWriteModule,
  Reader,
  Writer,
  ArrayReader,
  readWriteModule
}
import scala.reflect.ClassTag
import scala.quoted.*
import fr.hammons.slinc.modules.TransitionModule
import fr.hammons.slinc.modules.{ArgumentTransition, ReturnTransition}
import scala.NonEmptyTuple
import scala.language.implicitConversions

/** Describes types used by C interop
  */
sealed trait TypeDescriptor:
  self =>
  type Inner
  given DescriptorOf[Inner] with
    val descriptor = self
  def size(using dm: DescriptorModule): Bytes = dm.sizeOf(this)
  def alignment(using dm: DescriptorModule): Bytes = dm.alignmentOf(this)
  def toCarrierType(using dm: DescriptorModule): Class[?] =
    dm.toCarrierType(this)

  val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner]
  val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner]
  val argumentTransition: (
      TransitionModule,
      ReadWriteModule,
      Allocator
  ) ?=> ArgumentTransition[Inner]
  val returnTransition: (
      TransitionModule,
      ReadWriteModule
  ) ?=> ReturnTransition[Inner]

  val arrayReader
      : (ReadWriteModule, DescriptorModule, ClassTag[Inner]) ?=> ArrayReader[
        Inner
      ] =
    val reader = this.reader
    val size = this.size
    (mem, offset, num) => {
      var i = 0
      val array = Array.ofDim[Inner](num)
      while i < num do
        array(i) = reader(mem, size * i + offset)
        i += 1
      array
    }

  val arrayWriter
      : (ReadWriteModule, DescriptorModule) ?=> Writer[Array[Inner]] =
    val writer = this.writer
    val size = this.size
    (mem, offset, a) =>
      var i = 0
      while i < a.length do
        writer(mem, size * i + offset, a(i))
        i += 1
object TypeDescriptor:
  def fromTypeRepr(using q: Quotes)(
      typeRepr: q.reflect.TypeRepr
  ): Expr[TypeDescriptor] =
    import quotes.reflect.*

    val descOf = typeRepr.asType match
      case '[a] =>
        Expr
          .summon[DescriptorOf[a]]
          .getOrElse(
            report.errorAndAbort(
              s"No Descriptor for ${typeRepr.show(using Printer.TypeReprShortCode)}"
            )
          )

    '{ $descOf.descriptor }

  inline val unusedImplicit = "msg=unused implicit parameter"

sealed trait BasicDescriptor extends TypeDescriptor:
  override val argumentTransition = identity

  override val returnTransition = _.asInstanceOf[Inner]

case object ByteDescriptor extends BasicDescriptor:
  type Inner = Byte
  override val reader = readWriteModule.byteReader
  override val writer = readWriteModule.byteWriter

case object ShortDescriptor extends BasicDescriptor:
  type Inner = Short
  val reader = readWriteModule.shortReader
  val writer = readWriteModule.shortWriter

case object IntDescriptor extends BasicDescriptor:
  type Inner = Int
  val reader = readWriteModule.intReader
  val writer = readWriteModule.intWriter

case object LongDescriptor extends BasicDescriptor:
  type Inner = Long
  val reader = readWriteModule.longReader
  val writer = readWriteModule.longWriter

case object FloatDescriptor extends BasicDescriptor:
  type Inner = Float
  val reader = readWriteModule.floatReader
  val writer = readWriteModule.floatWriter

case object DoubleDescriptor extends BasicDescriptor:
  type Inner = Double
  val reader = readWriteModule.doubleReader
  val writer = readWriteModule.doubleWriter

case object PtrDescriptor extends TypeDescriptor:
  type Inner = Ptr[?]
  override val reader = (mem, offset) =>
    Ptr(readWriteModule.memReader(mem, offset), Bytes(0))
  override val writer = (mem, offset, a) =>
    readWriteModule.memWriter(mem, offset, a.mem)

  override val argumentTransition = _.mem.asAddress

  override val returnTransition = o =>
    Ptr[Any](summon[TransitionModule].addressReturn(o), Bytes(0))

/** A descriptor of a member of a Struct
  *
  * @param descriptor
  *   The [[TypeDescriptor]] this member is representing
  * @param name
  *   The name of the member
  */
case class StructMemberDescriptor(descriptor: TypeDescriptor, name: String)

/** A descriptor for a C struct
  *
  * @param members
  *   The members of the struct
  * @param clazz
  *   The class that's an analog for this struct
  * @param transform
  *   A function for transforming a tuple into the analog of this Struct
  */
trait StructDescriptor(
    val members: List[StructMemberDescriptor],
    val clazz: Class[?],
    val transform: Tuple => Product
) extends TypeDescriptor

case class AliasDescriptor[A](val real: TypeDescriptor) extends TypeDescriptor:
  type Inner = A
  type RealInner = real.Inner

  given bkwd: Conversion[Inner, RealInner] with
    def apply(x: Inner): real.Inner = x.asInstanceOf[real.Inner]

  given fwd: Conversion[RealInner, Inner] with
    def apply(x: real.Inner): Inner = x.asInstanceOf[Inner]

  val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner] =
    (rwm, _) ?=> (mem, bytes) => rwm.read(mem, bytes, real)

  val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner] =
    (rwm, _) ?=> (mem, bytes, a) => rwm.write(mem, bytes, real, a)

  override val argumentTransition =
    summon[TransitionModule].methodArgument(real, _, summon[Allocator])

  override val returnTransition = summon[TransitionModule].methodReturn(real, _)
  override def size(using dm: DescriptorModule): Bytes = dm.sizeOf(real)
  override def alignment(using dm: DescriptorModule): Bytes =
    dm.alignmentOf(real)
  override def toCarrierType(using dm: DescriptorModule): Class[?] =
    dm.toCarrierType(real)

case object VaListDescriptor extends TypeDescriptor:
  type Inner = VarArgs

  override val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner] =
    (mem, offset) => summon[ReadWriteModule].memReader(mem, offset).asVarArgs

  override val argumentTransition
      : (TransitionModule, ReadWriteModule, Allocator) ?=> ArgumentTransition[
        Inner
      ] = _.mem.asAddress

  override val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner] =
    (mem, offset, value) =>
      summon[ReadWriteModule].memWriter(mem, offset, value.mem)

  override val returnTransition
      : (TransitionModule, ReadWriteModule) ?=> ReturnTransition[Inner] = o =>
    summon[TransitionModule].addressReturn(o).asVarArgs

case class CUnionDescriptor(possibleTypes: Set[TypeDescriptor])
    extends TypeDescriptor:
  type Inner = CUnion[? <: NonEmptyTuple]

  override val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner] =
    summon[ReadWriteModule].unionReader(this)

  override val returnTransition
      : (TransitionModule, ReadWriteModule) ?=> ReturnTransition[Inner] = obj =>
    summon[ReadWriteModule]
      .unionReader(this)(summon[TransitionModule].memReturn(obj), Bytes(0))

  override val argumentTransition
      : (TransitionModule, ReadWriteModule, Allocator) ?=> ArgumentTransition[
        Inner
      ] = (i: Inner) => i.mem.asBase

  override val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner] =
    summon[ReadWriteModule].unionWriter(this)

case class SetSizeArrayDescriptor(
    val contained: TypeDescriptor,
    val number: Int
)(using ClassTag[contained.Inner])
    extends TypeDescriptor:

  override val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner] =
    (mem, offset) =>
      new SetSizeArray(
        summon[ReadWriteModule].readArray[contained.Inner](mem, offset, number)
      )

  override val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner] =
    (mem, offset, value) =>
      summon[ReadWriteModule]
        .writeArray[contained.Inner](mem, offset, value.toArray)

  override val argumentTransition
      : (TransitionModule, ReadWriteModule, Allocator) ?=> ArgumentTransition[
        Inner
      ] = arg =>
    val mem = summon[Allocator].allocate(this, 1)
    summon[ReadWriteModule].write(
      mem,
      Bytes(0),
      this,
      arg
    )
    mem.asAddress

  override val returnTransition
      : (TransitionModule, ReadWriteModule) ?=> ReturnTransition[Inner] =
    obj =>
      val mem = summon[TransitionModule].addressReturn(obj)
      summon[ReadWriteModule].read(mem, Bytes(0), this)

  type Inner = SetSizeArray[contained.Inner, ?]

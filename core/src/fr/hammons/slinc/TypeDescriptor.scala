package fr.hammons.slinc

import modules.DescriptorModule
import fr.hammons.slinc.modules.{
  ReadWriteModule,
  Reader,
  Writer,
  ArrayReader,
  readWriteModule
}
import scala.annotation.nowarn
import scala.reflect.ClassTag
import scala.quoted.*

/** Describes types used by C interop
  */
sealed trait TypeDescriptor:
  type Inner
  def size(using dm: DescriptorModule): Bytes = dm.sizeOf(this)
  def alignment(using dm: DescriptorModule): Bytes = dm.alignmentOf(this)
  def toCarrierType(using dm: DescriptorModule): Class[?] =
    dm.toCarrierType(this)

  val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner]
  val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner]

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
  @nowarn("msg=unused implicit parameter")
  @nowarn("msg=unused local definition")
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

sealed trait RealTypeDescriptor extends TypeDescriptor

case object ByteDescriptor extends RealTypeDescriptor:
  type Inner = Byte
  @nowarn("msg=unused implicit parameter")
  override val reader = readWriteModule.byteReader
  @nowarn("msg=unused implicit parameter")
  override val writer = readWriteModule.byteWriter

case object ShortDescriptor extends RealTypeDescriptor:
  type Inner = Short
  @nowarn("msg=unused implicit parameter")
  val reader = readWriteModule.shortReader
  @nowarn("msg=unused implicit parameter")
  val writer = readWriteModule.shortWriter

case object IntDescriptor extends RealTypeDescriptor:
  type Inner = Int
  @nowarn("msg=unused implicit parameter")
  val reader = readWriteModule.intReader
  @nowarn("msg=unused implicit parameter")
  val writer = readWriteModule.intWriter

case object LongDescriptor extends RealTypeDescriptor:
  type Inner = Long
  @nowarn("msg=unused implicit parameter")
  val reader = readWriteModule.longReader
  @nowarn("msg=unused implicit parameter")
  val writer = readWriteModule.longWriter

case object FloatDescriptor extends RealTypeDescriptor:
  type Inner = Float
  @nowarn("msg=unused implicit parameter")
  val reader = readWriteModule.floatReader
  @nowarn("msg=unused implicit parameter")
  val writer = readWriteModule.floatWriter

case object DoubleDescriptor extends RealTypeDescriptor:
  type Inner = Double
  @nowarn("msg=unused implicit parameter")
  val reader = readWriteModule.doubleReader
  @nowarn("msg=unused implicit parameter")
  val writer = readWriteModule.doubleWriter

case object PtrDescriptor extends RealTypeDescriptor:
  type Inner = Ptr[?]
  @nowarn("msg=unused implicit parameter")
  override val reader = (mem, offset) =>
    Ptr(readWriteModule.memReader(mem, offset), Bytes(0))
  @nowarn("msg=unused implicit parameter")
  override val writer = (mem, offset, a) =>
    readWriteModule.memWriter(mem, offset, a.mem)

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
) extends RealTypeDescriptor

case class AliasDescriptor[A](val real: RealTypeDescriptor)
    extends TypeDescriptor:
  type Inner = A
  type RealInner = real.Inner

  given bkwd: Conversion[Inner, RealInner] with
    def apply(x: Inner): real.Inner = x.asInstanceOf[real.Inner]

  given fwd: Conversion[RealInner, Inner] with
    def apply(x: real.Inner): Inner = x.asInstanceOf[Inner]

  val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[Inner] =
    (rwm, _) ?=> (mem, bytes) => rwm.readAlias(mem, bytes, real)

  val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[Inner] =
    (rwm, _) ?=> (mem, bytes, a) => rwm.writeAlias(mem, bytes, real, a)

  override def size(using dm: DescriptorModule): Bytes = dm.sizeOf(real)
  override def alignment(using dm: DescriptorModule): Bytes =
    dm.alignmentOf(real)
  override def toCarrierType(using dm: DescriptorModule): Class[?] =
    dm.toCarrierType(real)

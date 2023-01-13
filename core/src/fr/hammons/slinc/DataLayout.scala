package fr.hammons.slinc

import scala.deriving.Mirror

enum ByteOrder:
  case BigEndian
  case LittleEndian
  case HostDefault

sealed trait DataLayout:
  val name: Option[String]
  val size: Bytes
  val alignment: Bytes
  val byteOrder: ByteOrder
  def withName(name: String): DataLayout

  override def toString(): String = s"${this.getClass()}: {name: $name, size: ${size}, alignment: ${alignment}, byteOrder: ${byteOrder}}"

sealed trait PrimitiveLayout extends DataLayout:
  val alignment = size

class IntLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): IntLayout = IntLayout(Some(name), size, byteOrder)

object IntLayout:
  def unapply(l: IntLayout): (Option[String], Bytes, ByteOrder) =
    (l.name, l.size, l.byteOrder)

class LongLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): LongLayout =
    LongLayout(Some(name), size, byteOrder)

object LongLayout:
  def unapply(l: LongLayout): (Option[String], Bytes, ByteOrder) =
    (l.name, l.size, l.byteOrder)

class FloatLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): FloatLayout =
    FloatLayout(Some(name), size, byteOrder)

class DoubleLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): DoubleLayout =
    DoubleLayout(Some(name), size, byteOrder)

class ByteLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): ByteLayout =
    ByteLayout(Some(name), size, byteOrder)

class ShortLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): ShortLayout =
    ShortLayout(Some(name), size, byteOrder)

class PointerLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val byteOrder: ByteOrder
) extends PrimitiveLayout:
  def withName(name: String): PointerLayout =
    PointerLayout(Some(name), size, byteOrder)

object PointerLayout:
  def unapply(p: PointerLayout): (Option[String], Bytes, ByteOrder) =
    (p.name, p.size, p.byteOrder)

class PaddingLayout(
  val size: Bytes,
  val byteOrder: ByteOrder,
  val name: Option[String] = None
) extends DataLayout:
  val alignment: Bytes = Bytes(1)
  def withName(name: String): DataLayout = throw Error("Stop that")

case class StructMember(layout: DataLayout, name: Option[String], offset: Bytes)
class StructLayout private[slinc] (
    val name: Option[String],
    val size: Bytes,
    val alignment: Bytes,
    val byteOrder: ByteOrder,
    val transform: Tuple => Product,
    val clazz: Class[?],
    val children: Vector[StructMember]
) extends DataLayout:
  def withName(name: String): StructLayout = new StructLayout(
    Some(name),
    size,
    alignment,
    byteOrder,
    transform,
    clazz,
    children
  )

  def offsets: IArray[Bytes] = IArray.from(children.collect{
    case StructMember(_, Some(_), offset) => offset
  })


object StructLayout:
  def unapply(s: StructLayout): (Option[String], Bytes, Vector[StructMember]) =
    (s.name, s.size, s.children)

trait UnionLayout extends DataLayout:
  val layouts: IArray[DataLayout]

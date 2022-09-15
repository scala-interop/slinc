package fr.hammons.sffi

enum Kind:
  case Int
  case Short
  case Long
  case Float
  case Byte
  case Double
  case Pointer

sealed trait DataLayout:
  val name: Option[String]
  val size: Bytes
  def withName(name: String): DataLayout

sealed trait PrimitiveLayout extends DataLayout:
  val kind: Kind

trait IntLayout extends PrimitiveLayout:
  def withName(name: String): IntLayout
  val kind = Kind.Int

object IntLayout:
  def unapply(l: IntLayout): (Option[String], Bytes) = (l.name, l.size)

trait LongLayout extends PrimitiveLayout:
  def withName(name: String): LongLayout
  val kind = Kind.Long

trait FloatLayout extends PrimitiveLayout:
  def withName(name: String): FloatLayout
  val kind = Kind.Float

trait DoubleLayout extends PrimitiveLayout:
  def withName(name: String): DoubleLayout
  val kind = Kind.Double

trait ByteLayout extends PrimitiveLayout:
  def withName(name: String): ByteLayout
  val kind = Kind.Byte

trait ShortLayout extends PrimitiveLayout:
  def withName(name: String): ShortLayout
  val kind = Kind.Short

trait PointerLayout extends PrimitiveLayout:
  def withName(name: String): PointerLayout
  val kind = Kind.Pointer

case class StructMember(layout: DataLayout, name: String, offset: Bytes)
trait StructLayout extends DataLayout:
  val offsets: Vector[Bytes] = children.map(_.offset)
  val children: Vector[StructMember]
  def withName(name: String): StructLayout

object StructLayout:
  def unapply(s: StructLayout): (Option[String], Bytes, Vector[StructMember]) =
    (s.name, s.size, s.children)

trait UnionLayout extends DataLayout:
  val layouts: IArray[DataLayout]

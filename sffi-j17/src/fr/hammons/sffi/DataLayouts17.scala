package fr.hammons.sffi

import jdk.incubator.foreign.CLinker.{C_CHAR, C_INT}
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.ValueLayout
import jdk.incubator.foreign.MemoryLayout, MemoryLayout.PathElement

import scala.language.unsafeNulls
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.deriving.Mirror

class ByteLayout17(
    val name: Option[String] = None,
    val backing: ValueLayout = C_CHAR
) extends ByteLayout:
  def withName(name: String): ByteLayout =
    ByteLayout17(Some(name), backing.withName(name))
  val size: Bytes = Bytes(backing.byteSize())

class IntLayout17(
    val name: Option[String] = None,
    val backing: ValueLayout = C_INT
) extends IntLayout:
  def withName(name: String): IntLayout =
    IntLayout17(Some(name), backing.withName(name))
  val size: Bytes = Bytes(backing.byteSize())

class StructLayout17(
    val children: Vector[StructMember],
    val backing: GroupLayout,
    val transform: Tuple => Product,
    val clazz: Class[?],
    val name: Option[String] = None
) extends StructLayout:
  val size = Bytes(backing.byteSize())
  def withName(name: String): StructLayout =
    new StructLayout17(
      children,
      backing.withName(name),
      transform,
      clazz,
      Some(name)
    )

object StructLayout17:
  def apply[T](
      memberLayouts: DataLayout*
  )(using ev: ClassTag[T], m: Mirror.ProductOf[T]) =
    val backing: Seq[MemoryLayout] = memberLayouts.map {
      case i: IntLayout17    => i.backing: MemoryLayout
      case b: ByteLayout17   => b.backing: MemoryLayout
      case s: StructLayout17 => s.backing: MemoryLayout
      case _                 => throw Exception("I do not know this datatype!!")
    }

    val groupLayout = MemoryLayout.structLayout(backing*)

    val members =
      val paths = groupLayout
        .memberLayouts()
        .asScala
        .map(_.name().get())
        .map(PathElement.groupElement)
      paths
        .map(groupLayout.byteOffset(_))
        .map(Bytes.apply)
        .zip(memberLayouts)
        .map((offset, layout) => StructMember(layout, layout.name.get, offset))

    new StructLayout17(
      members.toVector,
      groupLayout,
      m.fromProduct(_).asInstanceOf[Product],
      ev.runtimeClass,
      None
    )

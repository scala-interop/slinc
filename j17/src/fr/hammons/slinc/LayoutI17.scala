package fr.hammons.slinc

import scala.reflect.ClassTag

import scala.deriving.Mirror.ProductOf
import jdk.incubator.foreign.CLinker.*
import jdk.incubator.foreign.{MemoryLayout, ValueLayout, GroupLayout},
MemoryLayout.PathElement
import scala.util.chaining.*
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*

object LayoutI17 extends LayoutI.PlatformSpecific:
  override val floatLayout: FloatLayout =
    FloatLayout(None, Bytes(C_FLOAT.nn.byteSize()), ByteOrder.HostDefault)

  override def getStructLayout[T](
      layouts: DataLayout*
  )(using po: ProductOf[T], ct: ClassTag[T]): StructLayout =
    val gl = MemoryLayout
      .structLayout(layouts.map(dataLayout2MemoryLayout)*)
      .nn

    val members =
      val paths = gl
        .memberLayouts()
        .nn
        .asScala
        .map(_.name().nn.get())
        .map(PathElement.groupElement)
      paths
        .map(gl.byteOffset(_))
        .map(Bytes.apply)
        .zip(layouts)
        .map((offset, layout) => StructMember(layout, layout.name.get, offset))

    StructLayout(
      gl.name().nn.toScala,
      Bytes(gl.byteSize()),
      Bytes(gl.byteAlignment()),
      ByteOrder.HostDefault,
      po.fromProduct(_).asInstanceOf[Product],
      ct.runtimeClass,
      members.toVector
    )

  // todo: support byte orders, different alignments
  def dataLayout2MemoryLayout(dataLayout: DataLayout): MemoryLayout =
    dataLayout match
      case IntLayout(name, size, order) =>
        name match
          case Some(name) =>
            C_INT.nn.withName(name).nn
          case None =>
            C_INT.nn
      case StructLayout(name, size, children) =>
        val childLayouts = children.map {
          case StructMember(childLayout, name, _) =>
            dataLayout2MemoryLayout(childLayout).withName(name).nn
        }
        val base = MemoryLayout.structLayout(childLayouts*).nn
        name match 
          case Some(name) => base.withName(name).nn
          case _ => base
  override val pointerLayout: PointerLayout =
    PointerLayout(None, Bytes(C_POINTER.nn.byteSize()), ByteOrder.HostDefault)

  override val shortLayout: ShortLayout =
    ShortLayout(None, Bytes(C_SHORT.nn.byteSize()), ByteOrder.HostDefault)

  override val intLayout: IntLayout =
    IntLayout(None, Bytes(C_INT.nn.byteSize()), ByteOrder.HostDefault)

  override val doubleLayout: DoubleLayout =
    DoubleLayout(None, Bytes(C_DOUBLE.nn.byteSize()), ByteOrder.HostDefault)

  override val byteLayout: ByteLayout =
    ByteLayout(None, Bytes(C_CHAR.nn.byteSize()), ByteOrder.HostDefault)

  override val longLayout: LongLayout =
    LongLayout(None, Bytes(C_LONG_LONG.nn.byteSize()), ByteOrder.HostDefault)

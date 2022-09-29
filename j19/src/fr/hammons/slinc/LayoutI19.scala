package fr.hammons.slinc

import scala.reflect.ClassTag

import scala.deriving.Mirror.ProductOf

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import java.lang.foreign.MemoryLayout.PathElement

object LayoutI19 extends LayoutI.PlatformSpecific:

  def dataLayout2MemoryLayout(dataLayout: DataLayout): MemoryLayout = 
    dataLayout match 
      case IntLayout(name, size, order) =>
        name match 
          case Some(name) =>
            ValueLayout.JAVA_INT.nn.withName(name).nn
          case None => 
            JAVA_INT.nn

      case PointerLayout(name,_,_) =>
        name match 
          case Some(name) =>
            ADDRESS.nn.withName(name).nn
          case None =>
            ADDRESS.nn

      case LongLayout(name,_,_) =>
        name match 
          case Some(name) =>
            JAVA_LONG.nn.withName(name).nn
          case None => 
            JAVA_LONG.nn

      case StructLayout(name, size, children) =>
        val childLayouts = children.map{
          case StructMember(childLayout, name, _) => dataLayout2MemoryLayout(childLayout).withName(name).nn
        }
        val base = MemoryLayout.structLayout(childLayouts*).nn
        name match 
          case Some(name) => base.withName(name).nn
          case _ => base

  override val doubleLayout: DoubleLayout = DoubleLayout(None, Bytes(ValueLayout.JAVA_DOUBLE.nn.byteSize()), ByteOrder.HostDefault)

  override def getStructLayout[T](layouts: DataLayout*)(using po: ProductOf[T], ct: ClassTag[T]): StructLayout = 
    val gl = MemoryLayout.structLayout(layouts.map(dataLayout2MemoryLayout)*).nn

    val members = 
      val paths = gl.memberLayouts().nn.asScala.map(_.name().nn.get()).map(PathElement.groupElement)
      paths.map(gl.byteOffset(_)).map(Bytes.apply).zip(layouts).map((offset, layout) => StructMember(layout, layout.name.get, offset))

    StructLayout(
      gl.name().nn.toScala,
      Bytes(gl.byteSize()),
      Bytes(gl.byteAlignment()),
      ByteOrder.HostDefault,
      po.fromProduct(_).asInstanceOf[Product],
      ct.runtimeClass,
      members.toVector
    )

  override val pointerLayout: PointerLayout = PointerLayout(None, Bytes(ValueLayout.ADDRESS.nn.byteSize()), ByteOrder.HostDefault)

  override val intLayout: IntLayout = IntLayout(None, Bytes(ValueLayout.JAVA_INT.nn.byteSize()), ByteOrder.HostDefault)

  override val shortLayout: ShortLayout = ShortLayout(None, Bytes(ValueLayout.JAVA_SHORT.nn.byteSize()), ByteOrder.HostDefault)

  override val longLayout: LongLayout = LongLayout(None, Bytes(ValueLayout.JAVA_LONG.nn.byteSize()), ByteOrder.HostDefault)

  override val floatLayout: FloatLayout = FloatLayout(None, Bytes(ValueLayout.JAVA_FLOAT.nn.byteSize()), ByteOrder.HostDefault)

  override val byteLayout: ByteLayout = ByteLayout(None, Bytes(ValueLayout.JAVA_BYTE.nn.byteSize()), ByteOrder.HostDefault)

  override def toCarrierType(dataLayout: DataLayout): Class[?] = dataLayout match 
    case _: IntLayout => ValueLayout.JAVA_INT.nn.carrier().nn
    case _: LongLayout => ValueLayout.JAVA_LONG.nn.carrier().nn
    case _: PointerLayout => ValueLayout.ADDRESS.nn.carrier().nn
    case _: StructLayout => classOf[MemorySegment]
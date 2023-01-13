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

      case PointerLayout(name, _, _) =>
        name match
          case Some(name) =>
            ADDRESS.nn.withName(name).nn
          case None =>
            ADDRESS.nn

      case LongLayout(name, _, _) =>
        name match
          case Some(name) =>
            JAVA_LONG.nn.withName(name).nn
          case None =>
            JAVA_LONG.nn

      case s: ShortLayout =>
        s.name match
          case None        => JAVA_SHORT.nn
          case Some(value) => JAVA_SHORT.nn.withName(value).nn

      case b: ByteLayout =>
        b.name match
          case None        => JAVA_BYTE.nn
          case Some(value) => JAVA_BYTE.nn.withName(value).nn

      case f: FloatLayout =>
        f.name match
          case None        => JAVA_FLOAT.nn
          case Some(value) => JAVA_FLOAT.nn.withName(value).nn

      case d: DoubleLayout =>
        d.name match
          case None        => JAVA_DOUBLE.nn
          case Some(value) => JAVA_DOUBLE.nn.withName(value).nn

      case p: PaddingLayout =>
        p.name match
          case None => MemoryLayout.paddingLayout(p.size.toLong * 8).nn
          case Some(value) => throw Error("Why are you trying to name a Padding layout???")
        

      case StructLayout(name, size, children) =>
        val childLayouts = children.map {
          case StructMember(childLayout, Some(name), _) =>
            dataLayout2MemoryLayout(childLayout).withName(name).nn
          case StructMember(childLayout, _, _) => 
            dataLayout2MemoryLayout(childLayout)
        }
        val base = MemoryLayout.structLayout(childLayouts*).nn
        name match
          case Some(name) => base.withName(name).nn
          case _          => base

  override val doubleLayout: DoubleLayout = DoubleLayout(
    None,
    Bytes(ValueLayout.JAVA_DOUBLE.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override val pointerLayout: PointerLayout = PointerLayout(
    None,
    Bytes(ValueLayout.ADDRESS.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override val intLayout: IntLayout = IntLayout(
    None,
    Bytes(ValueLayout.JAVA_INT.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override val shortLayout: ShortLayout = ShortLayout(
    None,
    Bytes(ValueLayout.JAVA_SHORT.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override val longLayout: LongLayout = LongLayout(
    None,
    Bytes(ValueLayout.JAVA_LONG.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override val floatLayout: FloatLayout = FloatLayout(
    None,
    Bytes(ValueLayout.JAVA_FLOAT.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override val byteLayout: ByteLayout = ByteLayout(
    None,
    Bytes(ValueLayout.JAVA_BYTE.nn.byteSize()),
    ByteOrder.HostDefault
  )

  override def toCarrierType(dataLayout: DataLayout): Class[?] =
    dataLayout match
      case _: ByteLayout    => ValueLayout.JAVA_BYTE.nn.carrier().nn
      case _: ShortLayout   => ValueLayout.JAVA_SHORT.nn.carrier().nn
      case _: IntLayout     => ValueLayout.JAVA_INT.nn.carrier().nn
      case _: LongLayout    => ValueLayout.JAVA_LONG.nn.carrier().nn
      case _: FloatLayout   => ValueLayout.JAVA_FLOAT.nn.carrier().nn
      case _: DoubleLayout  => ValueLayout.JAVA_DOUBLE.nn.carrier().nn
      case _: PointerLayout => ValueLayout.ADDRESS.nn.carrier().nn
      case _: StructLayout  => classOf[MemorySegment]
      case _: UnionLayout   => classOf[MemorySegment]

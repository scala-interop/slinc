package fr.hammons.slinc

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout, ValueLayout.*

object Mem19:
  val javaShort = JAVA_SHORT.nn.withBitAlignment(8)
  val javaInt = JAVA_INT.nn.withBitAlignment(8)
  val javaLong = JAVA_LONG.nn.withBitAlignment(8)
  val javaFloat = JAVA_FLOAT.nn.withBitAlignment(8)
  val javaDouble = JAVA_DOUBLE.nn.withBitAlignment(8)
  val javaByte = JAVA_BYTE.nn.withBitAlignment(8)

class Mem19(private[slinc] val mem: MemorySegment) extends Mem:
  import Mem19.*

  override def writeByteArray(v: Array[Byte], offset: Bytes): Unit =
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  def asAddress: Object = mem.address().nn

  def readDouble(offset: Bytes): Double = mem.get(javaDouble, offset.toLong)

  override def readInt(offset: Bytes): Int = mem.get(javaInt, offset.toLong)

  override def writeLong(v: Long, offset: Bytes): Unit =
    mem.set(javaLong, offset.toLong, v)

  override def writeIntArray(v: Array[Int], offset: Bytes): Unit =
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  override def offset(bytes: Bytes): Mem = Mem19(mem.asSlice(bytes.toLong).nn)

  override def writeInt(v: Int, offset: Bytes): Unit =
    mem.set(javaInt, offset.toLong, v)

  override def asBase: Object = mem

  override def readLong(offset: Bytes): Long = mem.get(javaLong, offset.toLong)

  override def readFloat(offset: Bytes): Float =
    mem.get(javaFloat, offset.toLong)

  override def readMem(offset: Bytes): Mem = ???

  override def resize(bytes: Bytes): Mem = Mem19(
    MemorySegment.ofAddress(mem.address().nn, bytes.toLong, mem.session()).nn
  )

  override def readByte(offset: Bytes): Byte = mem.get(javaByte, offset.toLong)

  override def writeFloat(v: Float, offset: Bytes): Unit =
    mem.set(javaFloat, offset.toLong, v)

  def writeByte(v: Byte, offset: Bytes): Unit =
    mem.set(javaByte, offset.toLong, v)

  def writeDouble(v: Double, offset: Bytes): Unit =
    mem.set(javaDouble, offset.toLong, v)

  def writeShort(v: Short, offset: Bytes): Unit =
    mem.set(javaShort, offset.toLong, v)

  override def readShort(offset: Bytes): Short =
    mem.get(javaShort, offset.toLong)

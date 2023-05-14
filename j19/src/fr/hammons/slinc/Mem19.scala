package fr.hammons.slinc

import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout, ValueLayout.*
import java.lang.foreign.Addressable
import java.lang.foreign.VaList

object Mem19:
  val javaShort = JAVA_SHORT.nn.withBitAlignment(8)
  val javaInt = JAVA_INT.nn.withBitAlignment(8)
  val javaChar = JAVA_CHAR.nn.withBitAlignment(8)
  val javaLong = JAVA_LONG.nn.withBitAlignment(8)
  val javaFloat = JAVA_FLOAT.nn.withBitAlignment(8)
  val javaDouble = JAVA_DOUBLE.nn.withBitAlignment(8)
  val javaByte = JAVA_BYTE.nn.withBitAlignment(8)
  val javaAddress = ADDRESS.nn.withBitAlignment(8)

class Mem19(private[slinc] val mem: MemorySegment) extends Mem:
  import Mem19.*

  override def copyFrom(other: Mem): Unit = other match
    case oMem: Mem19 => mem.copyFrom(oMem.mem).nn

  override def writeByteArray(v: Array[Byte], offset: Bytes): Unit =
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  def asAddress: Object = mem.address().nn

  def asVarArgs: VarArgs = VarArgs19(
    VaList.ofAddress(mem.address(), MemorySession.global()).nn
  )

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

  override def readAddress(offset: Bytes): Mem =
    val addr = mem.get(ADDRESS.nn, offset.toLong).nn

    Mem19(
      MemorySegment
        .ofAddress(
          addr,
          javaAddress.nn.byteSize(),
          MemorySession.global()
        )
        .nn
    )

  override def resize(bytes: Bytes): Mem = Mem19(
    resizeSegment(bytes)
  )

  def resizeSegment(to: Bytes): MemorySegment =
    MemorySegment.ofAddress(mem.address().nn, to.toLong, mem.session()).nn

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

  def writeAddress(v: Mem, offset: Bytes): Unit =
    mem.set(javaAddress, offset.toLong, v.asBase.asInstanceOf[Addressable])

  override def readIntArray(offset: Bytes, size: Int): Array[Int] =
    val arr = Array.ofDim[Int](size)
    val resizedMem = resizeSegment(Bytes(size * javaInt.nn.byteSize()))
    MemorySegment.ofArray(arr).nn.copyFrom(resizedMem)
    arr

package fr.hammons.slinc

import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.MemoryAccess
import jdk.incubator.foreign.CLinker.C_POINTER

class Mem17(private[slinc] val mem: MemorySegment) extends Mem:
  override def readDouble(offset: Bytes): Double =
    MemoryAccess.getDoubleAtOffset(mem, offset.toLong)

  override def asAddress: Object = mem.address().nn

  override def readLong(offset: Bytes): Long =
    MemoryAccess.getLongAtOffset(mem, offset.toLong)

  override def readFloat(offset: Bytes): Float =
    MemoryAccess.getFloatAtOffset(mem, offset.toLong)

  override def readShort(offset: Bytes): Short = MemoryAccess.getShortAtOffset(
    mem,
    offset.toLong
  )
  override def readMem(offset: Bytes): Mem = ???

  override def writeInt(v: Int, offset: Bytes): Unit =
    MemoryAccess.setIntAtOffset(mem, offset.toLong, v)

  override def writeIntArray(v: Array[Int], offset: Bytes): Unit =
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  override def readInt(offset: Bytes): Int =
    MemoryAccess.getIntAtOffset(mem, offset.toLong)

  override def writeLong(v: Long, offset: Bytes): Unit =
    MemoryAccess.setLongAtOffset(mem, offset.toLong, v)

  override def writeFloat(v: Float, offset: Bytes): Unit =
    MemoryAccess.setFloatAtOffset(mem, offset.toLong, v)

  override def writeByte(v: Byte, offset: Bytes): Unit =
    MemoryAccess.setByteAtOffset(mem, offset.toLong, v)

  override def writeByteArray(v: Array[Byte], offset: Bytes): Unit =
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  override def writeDouble(v: Double, offset: Bytes): Unit =
    MemoryAccess.setDoubleAtOffset(mem, offset.toLong, v)
  override def writeShort(v: Short, offset: Bytes): Unit =
    MemoryAccess.setShortAtOffset(mem, offset.toLong, v)

  override def readByte(offset: Bytes): Byte =
    MemoryAccess.getByteAtOffset(mem, offset.toLong)

  override def offset(bytes: Bytes): Mem = Mem17(mem.asSlice(bytes.toLong).nn)

  def asBase: Object = mem
  def resize(bytes: Bytes): Mem =
    Mem17(mem.address().nn.asSegment(bytes.toLong, mem.scope().nn).nn)

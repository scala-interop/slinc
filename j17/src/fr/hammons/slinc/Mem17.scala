package fr.hammons.slinc

import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.MemoryAccess

class Mem17(private[slinc] val mem: MemorySegment) extends Mem:

  override def readLong(offset: Bytes): Long = ???

  override def readFloat(offset: Bytes): Float = ???

  override def readShort(offset: Bytes): Short = ???

  override def readMem(offset: Bytes): Mem = ???

  override def writeInt(v: Int, offset: Bytes): Unit = MemoryAccess.setIntAtOffset(mem, offset.toLong, v)

  override def writeIntArray(v: Array[Int], offset: Bytes): Unit = 
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  override def readInt(offset: Bytes): Int = MemoryAccess.getIntAtOffset(mem, offset.toLong)

  override def write(v: Short, offset: Bytes): Unit = ???

  override def write(v: Mem, offset: Bytes): Unit = ???

  override def write(v: Byte, offset: Bytes): Unit = ???

  override def write(v: Int, offset: Bytes): Unit = ???

  override def writeLong(v: Long, offset: Bytes): Unit = ???

  override def writeFloat(v: Float, offset: Bytes): Unit = ???

  override def readByte(offset: Bytes): Byte = ???


  override def offset(bytes: Bytes): Mem = Mem17(mem)

  def asBase: Object = mem
  def resize(bytes: Bytes): Mem = 
    Mem17(mem.address().nn.asSegment(bytes.toLong, mem.scope().nn).nn)
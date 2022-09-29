package fr.hammons.slinc

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout, ValueLayout.*

class Mem19(private[slinc] val mem: MemorySegment) extends Mem:
  override def readInt(offset: Bytes): Int = mem.get(JAVA_INT, offset.toLong)

  override def writeLong(v: Long, offset: Bytes): Unit = ???

  override def writeIntArray(v: Array[Int], offset: Bytes): Unit =
    mem.asSlice(offset.toLong).nn.copyFrom(MemorySegment.ofArray(v))

  override def offset(bytes: Bytes): Mem = Mem19(mem.asSlice(bytes.toLong).nn)

  override def write(v: Short, offset: Bytes): Unit = ???

  override def write(v: Mem, offset: Bytes): Unit = ???

  override def write(v: Byte, offset: Bytes): Unit = ???

  override def write(v: Int, offset: Bytes): Unit = ???

  override def writeInt(v: Int, offset: Bytes): Unit =
    mem.set(JAVA_INT, offset.toLong, v)

  override def asBase: Object = mem

  override def readLong(offset: Bytes): Long = ???

  override def readFloat(offset: Bytes): Float = ???

  override def readMem(offset: Bytes): Mem = ???

  override def resize(bytes: Bytes): Mem = Mem19(
    MemorySegment.ofAddress(mem.address().nn, bytes.toLong, mem.session()).nn
  )

  override def readByte(offset: Bytes): Byte = ???

  override def writeFloat(v: Float, offset: Bytes): Unit = ???

  override def readShort(offset: Bytes): Short = ???
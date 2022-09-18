package fr.hammons.sffi

import jdk.incubator.foreign.MemorySegment

import jdk.incubator.foreign.MemoryAccess

final class Mem17(mem: MemorySegment) extends Mem:

  override def offset(bytes: Bytes): Mem = ???

  override final def readInt(offset: Bytes): Int = MemoryAccess.getIntAtOffset(mem, offset.toLong)

  override def readFloat(offset: Bytes): Float = ???

  override def readShort(offset: Bytes): Short = ???

  override def readByte(offset: Bytes): Byte = ???

  override def readLong(offset: Bytes): Long = MemoryAccess.getLongAtOffset(mem, offset.toLong)

  override def write(v: Short, offset: Bytes): Unit = ???

  override def write(v: Mem, offset: Bytes): Unit = ???

  override def write(v: Byte, offset: Bytes): Unit = ???

  override def writeFloat(v: Float, offset: Bytes): Unit = ???

  override def write(v: Int, offset: Bytes): Unit = MemoryAccess.setIntAtOffset(mem, offset.toLong, v)

  final def writeInt(v: Int, offset: Bytes): Unit = MemoryAccess.setIntAtOffset(mem, offset.toLong, v)

  final def writeLong(v: Long, offset: Bytes): Unit = MemoryAccess.setLongAtOffset(mem, offset.toLong, v)

  

  override def readMem(offset: Bytes): Mem = ???

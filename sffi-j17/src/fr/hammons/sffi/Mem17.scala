package fr.hammons.sffi

import jdk.incubator.foreign.MemorySegment

import jdk.incubator.foreign.MemoryAccess

class Mem17(mem: MemorySegment) extends Mem:

  override def offset(bytes: Bytes): Mem = ???

  override def readInt(offset: Bytes): Int = MemoryAccess.getIntAtOffset(mem, offset.toLong)

  override def readFloat(offset: Bytes): Float = ???

  override def readShort(offset: Bytes): Short = ???

  override def readByte(offset: Bytes): Byte = ???

  override def write(v: Short, offset: Bytes): Unit = ???

  override def write(v: Mem, offset: Bytes): Unit = ???

  override def write(v: Byte, offset: Bytes): Unit = ???

  override def write(v: Float, offset: Bytes): Unit = ???

  override def write(v: Int, offset: Bytes): Unit = MemoryAccess.setIntAtOffset(mem, offset.toLong, v)

  override def readMem(offset: Bytes): Mem = ???

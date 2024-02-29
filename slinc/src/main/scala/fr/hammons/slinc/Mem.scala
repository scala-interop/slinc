package fr.hammons.slinc

trait Mem:
  import scala.compiletime.asMatchable
  def offset(bytes: Bytes): Mem
  def resize(bytes: Bytes): Mem
  def asBase: Object
  def asAddress: Object
  def asVarArgs: VarArgs
  def copyFrom(other: Mem): Unit

  def writeFloat(v: Float, offset: Bytes): Unit
  def writeLong(v: Long, offset: Bytes): Unit
  def writeDouble(v: Double, offset: Bytes): Unit
  def writeShort(v: Short, offset: Bytes): Unit
  def writeByte(v: Byte, offset: Bytes): Unit
  def writeByteArray(v: Array[Byte], offset: Bytes): Unit
  def writeAddress(v: Mem, offset: Bytes): Unit

  def writeInt(v: Int, offset: Bytes): Unit
  def writeIntArray(v: Array[Int], offset: Bytes): Unit

  def readInt(offset: Bytes): Int
  def readFloat(offset: Bytes): Float
  def readByte(offset: Bytes): Byte
  def readAddress(offset: Bytes): Mem
  def readShort(offset: Bytes): Short
  def readLong(offset: Bytes): Long
  def readDouble(offset: Bytes): Double

  def readIntArray(offset: Bytes, size: Int): Array[Int]

package fr.hammons.sffi

trait Mem:
  def offset(bytes: Bytes): Mem

  def write(v: Int, offset: Bytes): Unit
  def write(v: Float, offset: Bytes): Unit
  def write(v: Byte, offset: Bytes): Unit
  // def write(v: Char, offset: Bytes): Unit
  def write(v: Mem, offset: Bytes): Unit
  def write(v: Short, offset: Bytes): Unit

  def write[A](v: A, offset: Bytes)(using Send[A]) =
    summon[Send[A]].to(this, offset, v)

  def readInt(offset: Bytes): Int 
  def readFloat(offset: Bytes): Float 
  def readByte(offset: Bytes): Byte
  def readMem(offset: Bytes): Mem
  def readShort(offset: Bytes): Short 
  def read[A](offset: Bytes)(using Receive[A]): A = summon[Receive[A]].from(this, offset)
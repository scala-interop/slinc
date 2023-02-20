package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import scala.collection.concurrent.TrieMap
import java.lang.invoke.MethodHandle
import scala.reflect.ClassTag

given readWriteModule17: ReadWriteModule with
  // todo: eliminate this
  val l17 = Library17(Slinc17.linker)
  val fnCache: TrieMap[FunctionDescriptor, Mem => ?] =
    TrieMap.empty

  val readerCache = DependentTrieMap[Reader]

  val arrayReaderCache = DependentTrieMap[ArrayReader]

  val writerCache = DependentTrieMap[Writer]

  val arrayWriterCache = DependentTrieMap[[I] =>> Writer[Array[I]]]

  val byteWriter = (mem, offset, value) => mem.writeByte(value, offset)
  val shortWriter = (mem, offset, value) => mem.writeShort(value, offset)

  val intWriter = (mem, offset, value) => mem.writeInt(value, offset)

  val longWriter = (mem, offset, value) => mem.writeLong(value, offset)

  val floatWriter = (mem, offset, value) => mem.writeFloat(value, offset)

  val doubleWriter = (mem, offset, value) => mem.writeDouble(value, offset)

  val memWriter = (mem, offset, value) => mem.writeAddress(value, offset)

  val byteReader = (mem, offset) => mem.readByte(offset)
  val shortReader = (mem, offset) => mem.readShort(offset)
  val intReader = (mem, offset) => mem.readInt(offset)
  val longReader = (mem, offset) => mem.readLong(offset)

  val floatReader = (mem, offset) => mem.readFloat(offset)
  val doubleReader = (mem, offset) => mem.readDouble(offset)

  val memReader = (mem, offset) => mem.readAddress(offset)

  arrayWriterCache
    .addOne(
      ByteDescriptor,
      (mem: Mem, offset: Bytes, value: Array[Byte]) =>
        mem.writeByteArray(value, offset)
    )
  arrayWriterCache
    .addOne(
      IntDescriptor,
      (mem: Mem, offset: Bytes, value: Array[Int]) =>
        mem.writeIntArray(value, offset)
    )

  override def read[A](memory: Mem, offset: Bytes)(using DescriptorOf[A]): A =
    val desc = DescriptorOf[A]
    readerCache.getOrElseUpdate(desc, desc.reader)(memory, offset)

  override def readFn[A](
      mem: Mem,
      descriptor: FunctionDescriptor,
      fn: => MethodHandle => Mem => A
  )(using Fn[A, ?, ?]): A =
    fnCache
      .getOrElseUpdate(descriptor, fn(l17.getDowncall(descriptor)))
      .asInstanceOf[Mem => A](mem)

  override def readArray[A](memory: Mem, offset: Bytes, size: Int)(using
      DescriptorOf[A],
      ClassTag[A]
  ): Array[A] =
    val desc = DescriptorOf[A]
    arrayReaderCache.getOrElseUpdate(desc, desc.arrayReader)(
      memory,
      offset,
      size
    )

  override def write[A](memory: Mem, offset: Bytes, value: A)(using
      DescriptorOf[A]
  ): Unit =
    val desc = DescriptorOf[A]
    writerCache.getOrElseUpdate(desc, desc.writer)(memory, offset, value)

  override def writeArray[A](memory: Mem, offset: Bytes, value: Array[A])(using
      DescriptorOf[A]
  ): Unit =
    val desc = DescriptorOf[A]
    arrayWriterCache.getOrElseUpdate(desc, desc.arrayWriter)(
      memory,
      offset,
      value
    )

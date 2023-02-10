package fr.hammons.slinc.modules

import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.*
import scala.reflect.ClassTag
import java.lang.invoke.MethodHandle

given readWriteModule19: ReadWriteModule with
  val l19 = Library19(Slinc19.linker)
  val writerCache: TrieMap[TypeDescriptor, (Mem, Bytes, ?) => Unit] =
    TrieMap.empty

  val arrayWriterCache: TrieMap[TypeDescriptor, (Mem, Bytes, ?) => Unit] =
    TrieMap.empty

  val readerCache: TrieMap[TypeDescriptor, (Mem, Bytes) => ?] = TrieMap.empty

  val arrayReaderCache: TrieMap[TypeDescriptor, (Mem, Bytes, Int) => ?] =
    TrieMap.empty

  val fnCache: TrieMap[FunctionDescriptor, Mem => ?] = TrieMap.empty

  registerWriter[Byte]((mem, offset, value) => mem.writeByte(value, offset))
  registerWriter[Short]((mem, offset, value) => mem.writeShort(value, offset))
  registerWriter[Int]((mem, offset, value) => mem.writeInt(value, offset))
  registerWriter[Long]((mem, offset, value) => mem.writeLong(value, offset))
  registerWriter[Float]((mem, offset, value) => mem.writeFloat(value, offset))
  registerWriter[Double]((mem, offset, value) => mem.writeDouble(value, offset))
  registerWriter[Ptr[Any]]((mem, offset, value) =>
    mem.writeAddress(value.mem, offset)
  )

  registerReader[Byte]((mem, offset) => mem.readByte(offset))
  registerReader[Short]((mem, offset) => mem.readShort(offset))
  registerReader[Int]((mem, offset) => mem.readInt(offset))
  registerReader[Long]((mem, offset) => mem.readLong(offset))
  registerReader[Float]((mem, offset) => mem.readFloat(offset))
  registerReader[Double]((mem, offset) => mem.readDouble(offset))
  registerReader[Ptr[Any]]((mem, offset) =>
    Ptr(mem.readAddress(offset), Bytes(0))
  )

  arrayWriterCache.addOne(
    ByteDescriptor,
    (mem: Mem, offset: Bytes, value: Array[Byte]) =>
      mem.writeByteArray(value, offset)
  )

  arrayWriterCache.addOne(
    IntDescriptor,
    (mem: Mem, offset: Bytes, value: Array[Int]) =>
      mem.writeIntArray(value, offset)
  )

  override def write[A](memory: Mem, offset: Bytes, value: A)(using
      DescriptorOf[A]
  ): Unit = writerCache(DescriptorOf[A])
    .asInstanceOf[(Mem, Bytes, A) => Unit](memory, offset, value)

  override def writeArray[A](memory: Mem, offset: Bytes, value: Array[A])(using
      DescriptorOf[A]
  ): Unit = arrayWriterCache(DescriptorOf[A])
    .asInstanceOf[(Mem, Bytes, Array[A]) => Unit](memory, offset, value)

  override def registerReader[A](fn: (Mem, Bytes) => A)(using
      DescriptorOf[A],
      ClassTag[A]
  ): Unit =
    readerCache.addOne(DescriptorOf[A], fn)
    arrayReaderCache.addOne(
      DescriptorOf[A],
      (mem: Mem, offset: Bytes, num: Int) =>
        var i = 0
        val buffer = Array.ofDim[A](num)
        val elemSize = DescriptorOf[A].size
        while i < num do
          buffer(i) = fn(mem, offset + (elemSize * i))
          i += 1
        buffer
    )

  override def read[A](memory: Mem, offset: Bytes)(using DescriptorOf[A]): A =
    readerCache(DescriptorOf[A]) match
      case fn: ((Mem, Bytes) => A) => fn(memory, offset)
      case fn => throw Error(s"$fn doesn't match the expected type!!")

  override def readFn[A](
      mem: Mem,
      descriptor: FunctionDescriptor,
      fn: => MethodHandle => Mem => ?
  )(using Fn[A, ?, ?]): A =
    fnCache.getOrElseUpdate(descriptor, fn(l19.getDowncall(descriptor))) match
      case upcall: (Mem => A) => upcall(mem)
      case huh =>
        throw Error(
          s"Value $huh doesn't match the type format expected. Please report this severe error!!"
        )

  override def readArray[A](memory: Mem, offset: Bytes, size: Int)(using
      DescriptorOf[A]
  ): Array[A] = arrayReaderCache(DescriptorOf[A]) match
    case fn: ((Mem, Bytes, Int) => Array[A]) => fn(memory, offset, size)
    case huh =>
      throw Error(
        s"Value $huh doesn't match the type format expected. Please report this severe error!!"
      )

  override def registerWriter[A](fn: (Mem, Bytes, A) => Unit)(using
      DescriptorOf[A]
  ): Unit =
    writerCache.addOne(DescriptorOf[A] -> fn)
    arrayWriterCache.addOne(
      DescriptorOf[A] -> ((mem: Mem, offset: Bytes, value: Array[A]) =>
        value.zipWithIndex.map((v, i) =>
          fn(mem, offset + DescriptorOf[A].size * i, v)
        )
      )
    )

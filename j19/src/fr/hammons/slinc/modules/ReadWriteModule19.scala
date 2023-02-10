package fr.hammons.slinc.modules

import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.*

given readWriteModule19: ReadWriteModule with
  val writerCache: TrieMap[TypeDescriptor, (Mem, Bytes, ?) => Unit] =
    TrieMap.empty

  val arrayWriterCache: TrieMap[TypeDescriptor, (Mem, Bytes, ?) => Unit] =
    TrieMap.empty

  registerWriter[Byte]((mem, offset, value) => mem.writeByte(value, offset))
  registerWriter[Short]((mem, offset, value) => mem.writeShort(value, offset))
  registerWriter[Int]((mem, offset, value) => mem.writeInt(value, offset))
  registerWriter[Long]((mem, offset, value) => mem.writeLong(value, offset))
  registerWriter[Float]((mem, offset, value) => mem.writeFloat(value, offset))
  registerWriter[Double]((mem, offset, value) => mem.writeDouble(value, offset))
  registerWriter[Ptr[Any]]((mem, offset, value) =>
    mem.writeAddress(value.mem, offset)
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
      DescriptorOf[A]
  ): Unit = ???

  override def read[A](memory: Mem, offset: Bytes)(using DescriptorOf[A]): A =
    ???

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

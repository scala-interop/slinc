package fr.hammons.slinc.modules

import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.*
import java.lang.invoke.MethodHandle
import scala.reflect.ClassTag
import fr.hammons.slinc.fnutils.Fn

private[slinc] given readWriteModule19: ReadWriteModule with

  override def unionWriter(
      td: TypeDescriptor
  ): Writer[CUnion[? <: NonEmptyTuple]] =
    val size = descriptorModule19.sizeOf(td.toForeignTypeDescriptor)
    (mem, offset, value) => mem.offset(offset).resize(size).copyFrom(value.mem)

  override def unionReader(
      td: TypeDescriptor
  ): Reader[CUnion[? <: NonEmptyTuple]] =
    val size = descriptorModule19.sizeOf(td.toForeignTypeDescriptor)
    (mem, offset) =>
      Scope19.createInferredScope(alloc ?=>
        val newMem = alloc.allocate(td.toForeignTypeDescriptor, 1)
        newMem.copyFrom(mem.offset(offset).resize(size))
        new CUnion(newMem)
      )

  val writerCache = DependentTrieMap[Writer]

  val arrayWriterCache = DependentTrieMap[[I] =>> Writer[Array[I]]]

  val readerCache = DependentTrieMap[Reader]

  val arrayReaderCache = DependentTrieMap[ArrayReader]

  val fnCache: TrieMap[CFunctionDescriptor, Mem => ?] = TrieMap.empty

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

  override def write(
      memory: Mem,
      offset: Bytes,
      typeDescriptor: TypeDescriptor,
      value: typeDescriptor.Inner
  ): Unit =
    writerCache.getOrElseUpdate(
      typeDescriptor,
      typeDescriptor.writer
    )(memory, offset, value)

  override def writeArray[A](memory: Mem, offset: Bytes, value: Array[A])(using
      DescriptorOf[A]
  ): Unit =
    val desc = DescriptorOf[A]
    arrayWriterCache.getOrElseUpdate(desc, desc.arrayWriter)(
      memory,
      offset,
      value
    )

  override def read(
      memory: Mem,
      offset: Bytes,
      typeDescriptor: TypeDescriptor
  ): typeDescriptor.Inner = readerCache.getOrElseUpdate(
    typeDescriptor,
    typeDescriptor.reader
  )(memory, offset)

  override def readFn[A](
      mem: Mem,
      descriptor: CFunctionDescriptor,
      fn: => MethodHandle => Mem => A
  )(using Fn[A, ?, ?]): A =
    fnCache.getOrElseUpdate(
      descriptor,
      fn(LinkageModule19.getDowncall(descriptor, Nil))
    ) match
      case upcall: (Mem => A) => upcall(mem)
      case huh =>
        throw Error(
          s"Value $huh doesn't match the type format expected. Please report this severe error!!"
        )

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

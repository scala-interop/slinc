package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.lang.invoke.MethodHandle
import scala.reflect.ClassTag

type Reader[A] = (Mem, Bytes) => A
type Writer[A] = (Mem, Bytes, A) => Unit
type ArrayReader[A] = (Mem, Bytes, Int) => Array[A]

val readWriteModule = (rwm: ReadWriteModule) ?=> rwm

trait ReadWriteModule:
  val byteReader: Reader[Byte]
  val byteWriter: Writer[Byte]
  val shortReader: Reader[Short]
  val shortWriter: Writer[Short]
  val intReader: Reader[Int]
  val intWriter: Writer[Int]
  val longReader: Reader[Long]
  val longWriter: Writer[Long]

  val floatReader: Reader[Float]
  val floatWriter: Writer[Float]
  val doubleReader: Reader[Double]
  val doubleWriter: Writer[Double]

  val memReader: Reader[Mem]
  val memWriter: Writer[Mem]

  def write(
      memory: Mem,
      offset: Bytes,
      typeDescriptor: TypeDescriptor,
      value: typeDescriptor.Inner
  ): Unit
  def writeArray[A](memory: Mem, offset: Bytes, value: Array[A])(using
      DescriptorOf[A]
  ): Unit

  def read(
      memory: Mem,
      offset: Bytes,
      typeDescriptor: TypeDescriptor
  ): typeDescriptor.Inner
  def readArray[A](memory: Mem, offset: Bytes, size: Int)(using
      DescriptorOf[A],
      ClassTag[A]
  ): Array[A]
  def readFn[A](
      mem: Mem,
      descriptor: CFunctionDescriptor,
      fn: => MethodHandle => Mem => A
  )(using Fn[A, ?, ?]): A

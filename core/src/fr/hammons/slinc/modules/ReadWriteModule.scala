package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.lang.invoke.MethodHandle
import scala.reflect.ClassTag
import scala.NonEmptyTuple
import fr.hammons.slinc.fnutils.Fn
import fr.hammons.slinc.jitc.OptimizableFn
import scala.quoted.Expr
import scala.quoted.Quotes

type Reader[A] = (Mem, Bytes) => A
type MemWriter[A] = (Mem, Bytes, A) => Unit
type ArrayReader[A] = (Mem, Bytes, Int) => Array[A]

val readWriteModule = (rwm: ReadWriteModule) ?=> rwm

trait ReadWriteModule:
  val byteReader: Reader[Byte]
  val byteWriter: MemWriter[Byte]
  val shortReader: Reader[Short]
  val shortWriter: MemWriter[Short]
  val intReader: Reader[Int]
  val intWriter: MemWriter[Int]
  val intWritingExpr: Quotes ?=> Expr[MemWriter[Int]]
  val longReader: Reader[Long]
  val longWriter: MemWriter[Long]

  val floatReader: Reader[Float]
  val floatWriter: MemWriter[Float]
  val doubleReader: Reader[Double]
  val doubleWriter: MemWriter[Double]

  val memReader: Reader[Mem]
  val memWriter: MemWriter[Mem]
  def unionReader(td: TypeDescriptor): Reader[CUnion[? <: NonEmptyTuple]]
  def unionWriter(td: TypeDescriptor): MemWriter[CUnion[? <: NonEmptyTuple]]

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

  def writeExpr(
      td: TypeDescriptor
  )(using Quotes, ClassTag[td.Inner]): Expr[MemWriter[Any]]
  def writeArrayExpr(td: TypeDescriptor)(using
      Quotes
  ): Expr[MemWriter[Array[Any]]]

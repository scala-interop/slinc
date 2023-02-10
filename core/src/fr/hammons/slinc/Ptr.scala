package fr.hammons.slinc

import scala.reflect.ClassTag
import fr.hammons.slinc.modules.DescriptorModule

class Ptr[A](private[slinc] val mem: Mem, private[slinc] val offset: Bytes):
  def `unary_!`(using receive: Receive[A]): A = receive.from(mem, offset)
  def asArray(size: Int)(using DescriptorOf[A], DescriptorModule)(using
      r: ReceiveBulk[A]
  ) =
    r.from(mem.resize(DescriptorOf[A].size * size), offset, size)

  def `unary_!_=`(value: A)(using send: Send[A]) = send.to(mem, offset, value)
  def apply(bytes: Bytes): Ptr[A] = Ptr[A](mem, offset + bytes)
  def apply(index: Int)(using DescriptorOf[A], DescriptorModule): Ptr[A] =
    Ptr[A](mem, offset + (DescriptorOf[A].size * index))

  def castTo[A]: Ptr[A] = this.asInstanceOf[Ptr[A]]
  private[slinc] def resize(toBytes: Bytes) =
    Ptr[A](mem.resize(toBytes), offset)

object Ptr:
  extension (p: Ptr[Byte])
    def copyIntoString(
        maxSize: Int
    )(using DescriptorOf[Byte], DescriptorModule) =
      var i = 0
      val resizedPtr = p.resize(Bytes(maxSize))
      while (i < maxSize && !resizedPtr(i) != 0) do i += 1

      String(resizedPtr.asArray(i).unsafeArray, "ASCII")
  def blank[A](using DescriptorOf[A], Allocator): Ptr[A] =
    this.blankArray[A](1)

  def blankArray[A](
      num: Int
  )(using descriptor: DescriptorOf[A], alloc: Allocator): Ptr[A] =
    Ptr[A](alloc.allocate(DescriptorOf[A], num), Bytes(0))

  def copy[A](
      a: Array[A]
  )(using alloc: Allocator, descriptor: DescriptorOf[A], send: Send[Array[A]]) =
    val mem = alloc.allocate(DescriptorOf[A], a.size)
    send.to(mem, Bytes(0), a)
    Ptr[A](mem, Bytes(0))

  def copy[A](using alloc: Allocator)(
      a: A
  )(using send: Send[A], descriptor: DescriptorOf[A]) =
    val mem = alloc.allocate(DescriptorOf[A], 1)
    send.to(mem, Bytes(0), a)
    Ptr[A](mem, Bytes(0))

  def copy(
      string: String
  )(using Allocator, DescriptorOf[Byte], Send[Array[Byte]]): Ptr[Byte] = copy(
    string.getBytes("ASCII").nn :+ 0.toByte
  )

  inline def upcall[A](inline a: A)(using alloc: Allocator): Ptr[A] =
    val nFn = Fn.toNativeCompatible(a)
    val descriptor = FunctionDescriptor.fromFunction[A]
    Ptr[A](alloc.upcall(descriptor, nFn), Bytes(0))

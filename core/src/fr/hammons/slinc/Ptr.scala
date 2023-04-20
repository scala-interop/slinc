package fr.hammons.slinc

import scala.reflect.ClassTag
import fr.hammons.slinc.modules.DescriptorModule
import fr.hammons.slinc.modules.ReadWriteModule

import scala.annotation.nowarn

class Ptr[A](private[slinc] val mem: Mem, private[slinc] val offset: Bytes):
  @nowarn("msg=unused import")
  inline def `unary_!`(using rwm: ReadWriteModule): A =
    import scala.compiletime.summonFrom

    summonFrom:
        case descO: DescriptorOf[A] =>
          rwm.read(mem, offset, descO.descriptor)
        case f: Fn[A, ?, ?] =>
          rwm.readFn(
            mem,
            FunctionDescriptor.fromFunction[A].toCFunctionDescriptor(),
            mh => MethodHandleTools.wrappedMH[A](_, mh)
          )

  @nowarn("msg=unused import")
  override def equals(x: Any): Boolean =
    import scala.compiletime.asMatchable
    x.asMatchable match
      case p: Ptr[?] =>
        p.mem.asAddress == this.mem.asAddress && p.offset == this.offset

  def asArray(size: Int)(using DescriptorOf[A], DescriptorModule)(using
      r: ReadWriteModule
  )(using ClassTag[A]): IArray[A] =
    IArray.unsafeFromArray(
      r.readArray(mem.resize(DescriptorOf[A].size * size), offset, size)
    )

  def `unary_!_=`(value: A)(using rwM: ReadWriteModule, desc: DescriptorOf[A]) =
    rwM.write(mem, offset, desc.descriptor, value)
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
    )(using DescriptorOf[Byte], DescriptorModule, ReadWriteModule) =
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
  )(using alloc: Allocator, descriptor: DescriptorOf[A], rwm: ReadWriteModule) =
    val mem = alloc.allocate(DescriptorOf[A], a.size)
    rwm.writeArray(mem, Bytes(0), a)
    Ptr[A](mem, Bytes(0))

  def copy[A](using alloc: Allocator)(
      a: A
  )(using
      rwm: ReadWriteModule,
      descriptor: DescriptorOf[A] {
        val descriptor: TypeDescriptor { type Inner = A }
      }
  ) =
    val mem = alloc.allocate(DescriptorOf[A], 1)
    rwm.write(mem, Bytes(0), descriptor.descriptor, a)
    Ptr[A](mem, Bytes(0))

  def copy(
      string: String
  )(using Allocator, DescriptorOf[Byte], ReadWriteModule): Ptr[Byte] = copy(
    string.getBytes("ASCII").nn :+ 0.toByte
  )

  inline def upcall[A](inline a: A)(using alloc: Allocator): Ptr[A] =
    val nFn = Fn.toNativeCompatible(a)
    val descriptor = FunctionDescriptor.fromFunction[A]
    Ptr[A](alloc.upcall(descriptor, nFn), Bytes(0))

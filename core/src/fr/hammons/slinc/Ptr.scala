package fr.hammons.slinc

import scala.util.TupledFunction
import scala.annotation.experimental
import scala.annotation.targetName
import scala.compiletime.{erasedValue, summonInline}

class Ptr[A](private[slinc] val mem: Mem, private[slinc] val offset: Bytes):
  def `unary_!`(using receive: Receive[A]) = receive.from(mem, offset)
  def asArray(size: Int)(using LayoutOf[A], Receive[A]) =
    for i <- 0 until size
    yield !this(i)

  def `unary_!_=`(value: A)(using send: Send[A]) = send.to(mem, offset, value)
  def apply(bytes: Bytes) = Ptr[A](mem, offset + bytes)
  def apply(index: Int)(using l: LayoutOf[A]) =
    Ptr[A](mem, l.layout.size * index)

object Ptr:
  def blank[A](using layout: LayoutOf[A], alloc: Allocator): Ptr[A] =
    this.blankArray[A](1)

  
  def blankArray[A](num: Int)(using layout: LayoutOf[A], alloc: Allocator): Ptr[A] =
    Ptr[A](alloc.allocate(layout.layout, num), Bytes(0))

  def copy[A](
      a: Array[A]
  )(using alloc: Allocator, layout: LayoutOf[A], send: Send[Array[A]]) =
    val mem = alloc.allocate(layout.layout, a.size)
    send.to(mem, Bytes(0), a)
    Ptr[A](mem, Bytes(0))

  def copy[A](using alloc: Allocator)(
      a: A
  )(using send: Send[A], layout: LayoutOf[A]) =
    val mem = alloc.allocate(layout.layout, 1)
    send.to(mem, Bytes(0), a)
    Ptr[A](mem, Bytes(0))

  inline def upcall[A](inline a: A)(using alloc: Allocator) =
    val nFn = Fn.toNativeCompatible(a)
    val descriptor = Descriptor.fromFunction[A]
    Ptr[A](alloc.upcall(descriptor, nFn), Bytes(0))

  // @experimental
  // inline def upcallExp[A, B <: Tuple, R, C](a: A)(using alloc: Allocator, fnTuple: TupledFunction[A,B => R], fnTuple2: TupledFunction[C, Tuple.Map[B, NativeOut] => NativeOut[R]]): Ptr[A] =
  //   val ret = inline erasedValue[R] match
  //     case _: Unit => None
  //     case _ => Some(summonInline[LayoutOf[R]].layout)

  //   Fn.contramap[A,NativeOut](a, [D] => (i: NativeOut[D]) =>
  //      i.asInstanceOf[D]
  //   )

  //   Ptr[A](alloc.upcall(LayoutI.tupLayouts[B], ret, a), Bytes(0))

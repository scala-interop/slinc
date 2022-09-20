package fr.hammons.slinc

class Ptr[A](mem: Mem, offset: Bytes):
  def `unary_!`(using receive: Receive[A]) = receive.from(mem, Bytes(0))
  def `unary_!_=`(value: A)(using send: Send[A]) = send.to(mem, Bytes(0), value)
  def apply(bytes: Bytes) = Ptr[A](mem, offset + bytes)

object Ptr:
  def blank[A](using layout: LayoutOf[A], alloc: Allocator) =
    Ptr[A](alloc.allocate(layout.layout), Bytes(0))

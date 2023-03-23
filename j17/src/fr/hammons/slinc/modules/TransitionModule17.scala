package fr.hammons.slinc.modules

import fr.hammons.slinc.TypeDescriptor

import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.*

import jdk.incubator.foreign.{MemoryAddress, MemorySegment}

given transitionModule17: TransitionModule with

  override def memReturn(value: Object): Mem = Mem17(
    value.asInstanceOf[MemorySegment]
  )

  override def methodArgument(m: Mem): Any = m.asBase

  private val maTransition: TrieMap[TypeDescriptor, Allocator ?=> ? => Any] =
    TrieMap(
      ByteDescriptor -> ((_: Allocator) ?=> (b: Byte) => b),
      ShortDescriptor -> ((_: Allocator) ?=> (s: Short) => s),
      IntDescriptor -> ((_: Allocator) ?=> (i: Int) => i),
      LongDescriptor -> ((_: Allocator) ?=> (l: Long) => l),
      FloatDescriptor -> ((_: Allocator) ?=> (f: Float) => f),
      DoubleDescriptor -> ((_: Allocator) ?=> (d: Double) => d),
      PtrDescriptor -> ((_: Allocator) ?=> (p: Ptr[?]) => p.mem.asAddress)
    )
  private val mrTransition: TrieMap[TypeDescriptor, Object => ?] = TrieMap(
    ByteDescriptor -> ((b: Object) => b.asInstanceOf[Byte]),
    ShortDescriptor -> ((s: Object) => s.asInstanceOf[Short]),
    IntDescriptor -> ((i: Object) => i.asInstanceOf[Int]),
    LongDescriptor -> ((l: Object) => l.asInstanceOf[Long]),
    FloatDescriptor -> ((f: Object) => f.asInstanceOf[Float]),
    DoubleDescriptor -> ((d: Object) => d.asInstanceOf[Double]),
    PtrDescriptor -> ((p: Object) =>
      Ptr[Any](
        Mem17(
          MemorySegment
            .globalNativeSegment()
            .nn
            .asSlice(p.asInstanceOf[MemoryAddress])
            .nn
        ),
        Bytes(0)
      )
    )
  )
  override def methodArgument(a: Allocator): Any = a.base

  override def methodArgument[A](
      td: TypeDescriptor,
      value: A,
      alloc: Allocator
  ): Any =
    val rtd = td match
      case ad: AliasDescriptor[?] => ad.real
      case _                      => td

    maTransition(rtd)(using alloc).asInstanceOf[A => Any](value)

  override def methodReturn[A](td: TypeDescriptor, value: Object): A =
    val rtd = td match
      case ad: AliasDescriptor[?] => ad.real
      case _                      => td

    mrTransition(rtd).asInstanceOf[Object => A](value)

  override def registerMethodArgumentTransition[A](
      td: TypeDescriptor,
      fn: (Allocator) ?=> A => Any
  ): Unit = maTransition.addOne(td -> fn)

  override def registerMethodReturnTransition[A](
      td: TypeDescriptor,
      fn: Object => A
  ): Unit = mrTransition.addOne(td -> fn)

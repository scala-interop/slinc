package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import scala.collection.concurrent.TrieMap
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.MemoryAddress

given transitionModule19: TransitionModule with
  private val maTransition: TrieMap[TypeDescriptor, Allocator ?=> ? => Any] =
    TrieMap(
      ByteDescriptor -> (_ ?=> (b: Byte) => b),
      ShortDescriptor -> (_ ?=> (s: Short) => s),
      IntDescriptor -> (_ ?=> (i: Int) => i),
      LongDescriptor -> (_ ?=> (l: Long) => l),
      FloatDescriptor -> (_ ?=> (f: Float) => f),
      DoubleDescriptor -> (_ ?=> (d: Double) => d),
      PtrDescriptor -> (_ ?=> (p: Ptr[?]) => p.mem.asAddress)
    )

  private val mrTransition: TrieMap[TypeDescriptor, Object => ?] = TrieMap(
    ByteDescriptor -> (_.asInstanceOf[Byte]),
    ShortDescriptor -> (_.asInstanceOf[Short]),
    IntDescriptor -> (_.asInstanceOf[Int]),
    LongDescriptor -> (_.asInstanceOf[Long]),
    FloatDescriptor -> (_.asInstanceOf[Float]),
    DoubleDescriptor -> (_.asInstanceOf[Double]),
    PtrDescriptor -> (p =>
      Ptr[Any](
        Mem19(
          MemorySegment
            .ofAddress(
              p.asInstanceOf[MemoryAddress],
              Int.MaxValue,
              MemorySession.global()
            )
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
  ): Any = maTransition(td)(using alloc).asInstanceOf[A => Any](value)

  def methodArgument(m: Mem): Any = m.asBase

  override def methodReturn[A](td: TypeDescriptor, value: Object): A =
    mrTransition(td).asInstanceOf[Object => A](value)

  override def registerMethodArgumentTransition[A](
      td: TypeDescriptor,
      fn: (Allocator) ?=> A => Any
  ): Unit = maTransition.addOne(td, fn)

  override def registerMethodReturnTransition[A](
      td: TypeDescriptor,
      fn: Object => A
  ): Unit = mrTransition.addOne(td, fn)

  override def memReturn(value: Object): Mem = Mem19(
    value.asInstanceOf[MemorySegment]
  )

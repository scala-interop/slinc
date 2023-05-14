package fr.hammons.slinc.modules

import fr.hammons.slinc.TypeDescriptor

import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.*

import jdk.incubator.foreign.{MemoryAddress, MemorySegment}

given transitionModule17: TransitionModule with

  override def memReturn(value: Object): Mem = Mem17(
    value.asInstanceOf[MemorySegment]
  )

  override def addressReturn(value: Object): Mem = Mem17(
    MemorySegment
      .globalNativeSegment()
      .nn
      .asSlice(value.asInstanceOf[MemoryAddress])
      .nn
  )

  override def methodArgument(m: Mem): Any = m.asBase

  private val maTransition: TrieMap[TypeDescriptor, Allocator ?=> ? => Any] =
    TrieMap.empty

  private val mrTransition: TrieMap[TypeDescriptor, Object => ?] = TrieMap.empty

  override def methodArgument(a: Allocator): Any = a.base

  override def methodArgument[A](
      td: TypeDescriptor,
      value: A,
      alloc: Allocator
  ): Any =
    maTransition
      .getOrElseUpdate(td, td.argumentTransition)(using alloc)
      .asInstanceOf[A => Any](value)

  override def methodReturn[A](td: TypeDescriptor, value: Object): A =
    mrTransition
      .getOrElseUpdate(td, td.returnTransition)
      .asInstanceOf[Object => A](value)

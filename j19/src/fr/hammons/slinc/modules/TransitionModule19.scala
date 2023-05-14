package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import scala.collection.concurrent.TrieMap
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.MemoryAddress
import java.lang.foreign.SegmentAllocator

given transitionModule19: TransitionModule with

  def addressReturn(obj: Object): Mem = Mem19(
    MemorySegment
      .ofAddress(
        obj.asInstanceOf[MemoryAddress],
        Int.MaxValue,
        MemorySession.global()
      )
      .nn
  )
  private val maTransition: TrieMap[TypeDescriptor, Allocator ?=> ? => Any] =
    TrieMap.empty

  private val mrTransition: TrieMap[TypeDescriptor, Object => ?] = TrieMap.empty

  override def methodArgument(a: Allocator): Any = a.base

  override def methodArgument[A](
      td: TypeDescriptor,
      value: A,
      allocator: Allocator
  ): Any =
    maTransition
      .getOrElseUpdate(td, td.argumentTransition)(using allocator)
      .asInstanceOf[A => Any](value)

  def methodArgument(m: Mem): Any = m.asBase

  override def methodReturn[A](td: TypeDescriptor, value: Object): A =
    mrTransition
      .getOrElseUpdate(td, td.returnTransition)
      .asInstanceOf[Object => A](value)

  override def memReturn(value: Object): Mem = Mem19(
    value.asInstanceOf[MemorySegment]
  )

  override def cUnionReturn(td: TypeDescriptor, value: Object): CUnion[?] =
    val session = MemorySession.openImplicit().nn
    val segmentAlloc = SegmentAllocator.newNativeArena(session).nn
    val segment =
      segmentAlloc.allocate(descriptorModule19.toMemoryLayout(td)).nn
    new CUnion(
      Mem19(
        segment.copyFrom(value.asInstanceOf[MemorySegment]).nn
      )
    )

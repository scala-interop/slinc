package io.gitlab.mhammons.slinc

import scala.reflect.ClassTag
import scala.compiletime.ops.int.<
import javax.naming.spi.DirStateFactory.Result
import components.NativeInfo
import jdk.incubator.foreign.{MemoryLayout, MemorySegment}

type InboundsProof[Size <: Singleton & Int, Index <: Singleton & Int, Result] =
   (Index < Size =:= true) ?=> Result
class StaticArray[T, Size <: Singleton & Int](
    array: Array[T],
    val size: Int
):
   def apply[Idx <: Singleton & Int](
       index: Idx
   ): InboundsProof[Size, Idx, T] = array(index)

   def apply(index: Int) = array(index)

   def update[Idx <: Singleton & Int](
       index: Idx,
       t: T
   ): InboundsProof[Size, Idx, Unit] = array(index) = t
   def update(index: Int, t: T) = array(index) = t
   def underlying = array

object StaticArray:
   def apply[T: ClassTag: NativeInfo, Size <: Singleton & Int: ValueOf] =
      new StaticArray[T, Size](
        Array.ofDim[T](valueOf[Size]),
        valueOf[Size]
      )

   given [T: NativeInfo, Size <: Singleton & Int: ValueOf]
       : NativeInfo[StaticArray[T, Size]] with
      val layout =
         MemoryLayout.sequenceLayout(valueOf[Size], NativeInfo[T].layout)
      val carrierType = classOf[MemorySegment]

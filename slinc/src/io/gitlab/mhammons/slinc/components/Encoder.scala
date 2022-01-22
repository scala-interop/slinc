package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemoryAccess,
   MemorySegment,
   MemoryAddress,
   ValueLayout
}

type Encodee[A, B] = Encoder[A] ?=> B
def encoderOf[A]: Encodee[A, Encoder[A]] = summon[Encoder[A]]
def encode[A](
    a: A,
    memoryAddress: MemoryAddress,
    offset: Long
): Encodee[A, Unit] = encoderOf[A].into(a, memoryAddress, offset)

//todo: rename to encoder
trait Encoder[A]:
   def into(a: A, memoryAddress: MemoryAddress, offset: Long): Unit
   def contramap[B](fn: B => A): Encoder[B] =
      val orig = this
      new Encoder[B]:
         def into(a: B, memoryAddress: MemoryAddress, offset: Long) =
            orig.into(fn(a), memoryAddress, offset)
object Encoder:
   given Encoder[Int] with
      def into(a: Int, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setIntAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Encoder[Long] with
      def into(a: Long, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setLongAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Encoder[Float] with
      def into(a: Float, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setFloatAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Encoder[Double] with
      def into(a: Double, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setDoubleAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Encoder[Short] with
      def into(a: Short, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setShortAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Encoder[Boolean] with
      def into(a: Boolean, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           if a then 1 else 0
         )

   given Encoder[Char] with
      def into(a: Char, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setCharAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )
   given Encoder[Byte] with
      def into(a: Byte, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given [A](using Encoder[A], NativeInfo[A]): Encoder[Array[A]] with
      def into(array: Array[A], memoryAddress: MemoryAddress, offset: Long) =
         var i = 0
         while i < array.length do
            encoderOf[A].into(
              array(i),
              memoryAddress,
              offset + (NativeInfo[A].layout.byteSize * i)
            )
            i += 1

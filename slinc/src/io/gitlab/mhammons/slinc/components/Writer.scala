package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemoryAccess,
   MemorySegment,
   MemoryAddress,
   ValueLayout
}
import scala.compiletime.erasedValue

type Writee[A, B] = Writer[A] ?=> B
def writerOf[A]: Writee[A, Writer[A]] = summon[Writer[A]]
def write[A](
    a: A,
    memoryAddress: MemoryAddress,
    offset: Long
): Writee[A, Unit] = writerOf[A].into(a, memoryAddress, offset)

//todo: rename to encoder
trait Writer[A]:
   def into(a: A, memoryAddress: MemoryAddress, offset: Long): Unit
   def contramap[B](fn: B => A): Writer[B] =
      val orig = this
      new Writer[B]:
         def into(a: B, memoryAddress: MemoryAddress, offset: Long) =
            orig.into(fn(a), memoryAddress, offset)
object Writer:
   given Writer[Int] with
      def into(a: Int, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setIntAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Writer[Long] with
      def into(a: Long, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setLongAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Writer[Float] with
      def into(a: Float, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setFloatAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Writer[Double] with
      def into(a: Double, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setDoubleAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Writer[Short] with
      def into(a: Short, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setShortAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Writer[Boolean] with
      def into(a: Boolean, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           if a then 1 else 0
         )

   given Writer[Char] with
      def into(a: Char, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setCharAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )
   given Writer[Byte] with
      def into(a: Byte, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   private inline def arrayToMemsegment[A](array: Array[A]) = inline array match
      case arr: Array[Byte]   => MemorySegment.ofArray(arr)
      case arr: Array[Short]  => MemorySegment.ofArray(arr)
      case arr: Array[Int]    => MemorySegment.ofArray(arr)
      case arr: Array[Long]   => MemorySegment.ofArray(arr)
      case arr: Array[Float]  => MemorySegment.ofArray(arr)
      case arr: Array[Double] => MemorySegment.ofArray(arr)

   private inline def specializedEncoderCopy[A](
       array: Array[A],
       memoryAddress: MemoryAddress,
       offset: Long
   )(using NativeInfo[A]) =
      memoryAddress
         .addOffset(offset)
         .asSegment(layoutOf[A].byteSize * array.length, memoryAddress.scope)
         .copyFrom(arrayToMemsegment(array))

   given byteArr: Writer[Array[Byte]] = specializedEncoderCopy(_, _, _)

   given shortArr: Writer[Array[Short]] = specializedEncoderCopy(_, _, _)

   given intArr: Writer[Array[Int]] = specializedEncoderCopy(_, _, _)

   given longArr: Writer[Array[Long]] = specializedEncoderCopy(_, _, _)

   given floatArr: Writer[Array[Float]] = specializedEncoderCopy(_, _, _)

   given doubleArr: Writer[Array[Double]] = specializedEncoderCopy(_, _, _)

   given [A](using Writer[A], NativeInfo[A]): Writer[Array[A]] with
      def into(array: Array[A], memoryAddress: MemoryAddress, offset: Long) =
         var i = 0
         while i < array.length do
            writerOf[A].into(
              array(i),
              memoryAddress,
              offset + (NativeInfo[A].layout.byteSize * i)
            )
            i += 1

package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemorySegment,
   MemoryAccess,
   MemoryAddress,
   CLinker,
   FunctionDescriptor,
   SegmentAllocator
}
import scala.util.chaining.*
import scala.compiletime.erasedValue
import java.lang.invoke.{MethodType => MT}
import io.gitlab.mhammons.polymorphics.VoidHelper
import io.gitlab.mhammons.polymorphics.MethodHandleHandler
import scala.quoted.*

type Readee[A, B] = Reader[A] ?=> B
def readerOf[A]: Readee[A, Reader[A]] =
   summon[Reader[A]]
trait Reader[A]:
   def from(memoryAddress: MemoryAddress, offset: Long): A

   def map[B](fn: A => B): Reader[B] =
      val orig = this
      new Reader[B]:
         def from(memoryAddress: MemoryAddress, offset: Long) = fn(
           orig.from(memoryAddress, offset)
         )
object Reader:
   inline def getAtOffset[A](
       inline fn: (MemorySegment, Long) => A,
       inline memoryAddress: MemoryAddress,
       inline offset: Long
   ) =
      fn(
        MemorySegment.globalNativeSegment,
        memoryAddress.toRawLongValue + offset
      )
   given Reader[Int] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getIntAtOffset, memoryAddress, offset)
   given Reader[Float] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getFloatAtOffset, memoryAddress, offset)

   given Reader[Long] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getLongAtOffset, memoryAddress, offset)

   given Reader[Short] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getShortAtOffset, memoryAddress, offset)

   given Reader[Byte] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getByteAtOffset, memoryAddress, offset)

   given Reader[Boolean] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         if getAtOffset(
              MemoryAccess.getByteAtOffset,
              memoryAddress,
              offset
            ) == 0
         then false
         else true

   given Reader[Char] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getCharAtOffset, memoryAddress, offset)

   private val paramNames =
      LazyList.iterate('a', 24)(c => (c.toInt + 1).toChar).map(_.toString)

   inline given [A](using
       Fn[A]
   ): Reader[A] = ${
      genDecoder[A]
   }

   private def genDecoder[A](using Quotes, Type[A]) =
      import quotes.reflect.*
      '{
         new Reader[A]:
            def from(memoryAddress: MemoryAddress, offset: Long) =
               ${
                  val (inputTypes, retType) = TypeRepr.of[A] match
                     case AppliedType(_, args) =>
                        val types = args.map(_.asType)
                        (types.init, types.last)


                  MethodHandleMacros.wrappedMH('memoryAddress, inputTypes.map(_ => None), inputTypes, retType).fold(msgs => report.errorAndAbort(msgs.mkString("\n")), a => a.asExprOf[A])
               }
      }

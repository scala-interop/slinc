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

type Deserializee[A, B] = Deserializer[A] ?=> B
def deserializerOf[A]: Deserializee[A, Deserializer[A]] =
   summon[Deserializer[A]]
//todo: rename to decoder
trait Deserializer[A]:
   def from(memoryAddress: MemoryAddress, offset: Long): A

   def map[B](fn: A => B): Deserializer[B] =
      val orig = this
      new Deserializer[B]:
         def from(memoryAddress: MemoryAddress, offset: Long) = fn(
           orig.from(memoryAddress, offset)
         )
object Deserializer:
   inline def getAtOffset[A](
       inline fn: (MemorySegment, Long) => A,
       inline memoryAddress: MemoryAddress,
       inline offset: Long
   ) =
      fn(
        MemorySegment.globalNativeSegment,
        memoryAddress.toRawLongValue + offset
      )
   given Deserializer[Int] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getIntAtOffset, memoryAddress, offset)
   given Deserializer[Float] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getFloatAtOffset, memoryAddress, offset)

   given Deserializer[Long] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getLongAtOffset, memoryAddress, offset)

   given Deserializer[Short] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getShortAtOffset, memoryAddress, offset)

   given Deserializer[Byte] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getByteAtOffset, memoryAddress, offset)

   given Deserializer[Boolean] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         if getAtOffset(
              MemoryAccess.getByteAtOffset,
              memoryAddress,
              offset
            ) == 0
         then false
         else true

   given Deserializer[Char] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         getAtOffset(MemoryAccess.getCharAtOffset, memoryAddress, offset)

   private val paramNames =
      LazyList.iterate('a', 24)(c => (c.toInt + 1).toChar).map(_.toString)

   inline given [A](using
       Fn[A]
   ): Deserializer[A] = ${
      genDeserializer[A]
   }

   private def genDeserializer[A](using Quotes, Type[A]) =
      import quotes.reflect.*
      '{
         new Deserializer[A]:
            def from(memoryAddress: MemoryAddress, offset: Long) =
               ${
                  val (inputTypes, retType) = TypeRepr.of[A] match
                     case AppliedType(_, args) =>
                        val types = args.map(_.asType)
                        (types.init, types.last)

                  Lambda(
                    Symbol.spliceOwner,
                    MethodType(paramNames.take(inputTypes.size).toList)(
                      _ => inputTypes.map { case '[a] => TypeRepr.of[a] },
                      _ => retType.pipe { case '[r] => TypeRepr.of[r] }
                    ),
                    (meth, params) =>
                       retType.pipe { case '[r] =>
                          MethodHandleMacros
                             .binding[r](
                               'memoryAddress,
                               params.map(_.asExpr).zip(inputTypes)
                             )
                             .asTerm
                             .changeOwner(meth)

                       }
                  ).asExprOf[A]

               }
      }

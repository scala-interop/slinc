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
object Deserializer:
   given Deserializer[Int] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.getIntAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset
         )
   given Deserializer[Float] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.getFloatAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset
         )

   given Deserializer[Long] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.getLongAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset
         )

   given Deserializer[Short] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.getShortAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset
         )

   given Deserializer[Byte] with
      def from(memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.getByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset
         )

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

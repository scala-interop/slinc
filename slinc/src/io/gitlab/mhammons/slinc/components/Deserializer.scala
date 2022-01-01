package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   MemorySegment,
   MemoryAccess,
   MemoryLayout,
   MemoryAddress
}, MemoryLayout.PathElement
import scala.quoted.*
import scala.util.chaining.*
import scala.deriving.Mirror
import io.gitlab.mhammons.slinc.Ptr
import io.gitlab.mhammons.slinc.StaticArray
import scala.reflect.ClassTag
type Id[A] = A
type Deserializable[A] = Deserializer[A] ?=> A
//todo: rename to decoder
trait Deserializer[A]:
   def from(memoryAddress: MemoryAddress, offset: Long): A
object Deserializer:
   def from[A](memoryAddress: MemoryAddress, offset: Long): Deserializable[A] =
      summon[Deserializer[A]].from(memoryAddress, offset)

   // type Deserializable[A] = (d: Deserializer[A, ?]) ?=> d.Cont[A]
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

   // todo: deserialize pointers into memory segments without grabbing the layout
   // val ptrDeserializer

   given Deserializer[Ptr[Any]] with
      def from(
          memoryAddress: MemoryAddress,
          offset: Long
      ): Ptr[Any] =
         val address = MemoryAccess.getAddressAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset
         )

         Ptr[Any](address, 0)

   given [A: ClassTag: NativeInfo: Deserializer, B <: Singleton & Int: ValueOf]
       : Deserializer[StaticArray[A, B]] =
      new Deserializer[StaticArray[A, B]]:
         def from(
             memoryAddress: MemoryAddress,
             offset: Long
         ): StaticArray[A, B] =
            val s = StaticArray[A, B]
            val len = valueOf[B]
            var i = 0
            val ni = NativeInfo[A]
            while i < len do
               s(i) = Deserializer
                  .from[A](memoryAddress, offset + (i * ni.layout.byteSize))
               i += 1
            s

   def fromTypeInfo(
       memorySegmentExpr: Expr[MemoryAddress],
       layout: Expr[MemoryLayout],
       path: Seq[Expr[PathElement]],
       typeInfo: TypeInfo
   )(using q: Quotes): q.reflect.Term =
      import quotes.reflect.*
      typeInfo match
         case PrimitiveInfo(name, '[a]) =>
            val updatedPath = Expr.ofSeq(path :+ '{
               PathElement.groupElement(${ Expr(name) })
            })
            '{
               ${ Expr.summonOrError[Deserializer[a]] }.from(
                 $memorySegmentExpr,
                 $layout.byteOffset($updatedPath*)
               )
            }.asTerm
         case ProductInfo(name, members, '[a]) =>
            val updatedPath =
               if name.isEmpty then path
               else path :+ '{ PathElement.groupElement(${ Expr(name) }) }
            Apply(
              Select(
                New(TypeTree.of[a]),
                TypeRepr.of[a].typeSymbol.primaryConstructor
              ),
              members
                 .map(m =>
                    fromTypeInfo(
                      memorySegmentExpr,
                      layout,
                      updatedPath,
                      m
                    )
                 )
                 .toList
            )

         case PtrInfo(name, _, t) =>
            fromTypeInfo(
              memorySegmentExpr,
              layout,
              path,
              PrimitiveInfo(name, t)
            )

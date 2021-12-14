package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryAccess, MemoryLayout},
MemoryLayout.PathElement
import scala.quoted.*
import scala.util.chaining.*
import scala.deriving.Mirror
import io.gitlab.mhammons.slinc.Ptr
//todo: rename to decoder
trait Deserializer[A]:
   def from(memorySegment: MemorySegment, offset: Long): A
object Deserializer:
   def primitive[A: Type](
       memorySegmentExpr: Expr[MemorySegment],
       byteOffsetExpr: Expr[Long]
   )(using Quotes): quotes.reflect.Term =
      import quotes.reflect.*
      Type.of[A] match
         case '[Int] =>
            '{
               MemAccess.getIntAtOffset(null, 0)
            }.asTerm
   given Deserializer[Int] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getIntAtOffset(memorySegment, offset)
   given Deserializer[Float] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getFloatAtOffset(memorySegment, offset)

   given Deserializer[Long] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getLongAtOffset(memorySegment, offset)

   given Deserializer[Short] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getShortAtOffset(memorySegment, offset)

   given Deserializer[Byte] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getByteAtOffset(memorySegment, offset)

   // todo: deserialize pointers into memory segments without grabbing the layout
   // val ptrDeserializer

   given [A](using underlying: LayoutOf[A]): Deserializer[Ptr[A]] with
      def from(memorySegment: MemorySegment, offset: Long) =
         val address = MemoryAccess.getAddressAtOffset(memorySegment, offset)

         Ptr(address.asSegment(underlying.layout.byteSize, address.scope), 0)

   def fromTypeInfo(
       memorySegmentExpr: Expr[MemorySegment],
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

         case PtrInfo(name, _, t) => fromTypeInfo(memorySegmentExpr, layout, path, PrimitiveInfo(name, t))

package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   SegmentAllocator,
   CLinker,
   MemoryAccess,
   MemorySegment,
   MemoryLayout,
   GroupLayout
}, CLinker.C_INT, MemoryLayout.PathElement

import io.gitlab.mhammons.slinc.Ptr

import scala.quoted.*
import scala.util.chaining.*

//todo: rename to encoder
trait Serializer[A]:
   def to(a: A)(using
       segAlloc: SegmentAllocator,
       layout: LayoutOf[A]
   ): Ptr[A] =
      val segment = segAlloc.allocate(layout.layout)
      into(a, segment, 0)
      layout.layout match
         case gl: GroupLayout =>
            gl.memberLayouts // TODO: generate Map from here
      Ptr[A](segment, 0)
   def into(a: A, memorySegment: MemorySegment, offset: Long): Unit

object Serializer:
   given Serializer[Int] with
      def into(a: Int, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setIntAtOffset(memorySegment, offset, a)

   given Serializer[Long] with
      def into(a: Long, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setLongAtOffset(memorySegment, offset, a)

   given Serializer[Float] with
      def into(a: Float, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setFloatAtOffset(memorySegment, offset, a)

   given Serializer[Double] with
      def into(a: Double, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setDoubleAtOffset(memorySegment, offset, a)

   given Serializer[Short] with
      def into(a: Short, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setShortAtOffset(memorySegment, offset, a)

   given Serializer[Boolean] with
      def into(a: Boolean, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setByteAtOffset(memorySegment, offset, if a then 1 else 0)

   given Serializer[Char] with
      def into(a: Char, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setCharAtOffset(memorySegment, offset, a)
   given Serializer[Byte] with
      def into(a: Byte, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setByteAtOffset(memorySegment, offset, a)

   private val ptrSerializer = new Serializer[Ptr[Any]]:
      def into(ptr: Ptr[Any], memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setAddressAtOffset(
           memorySegment,
           offset,
           ptr.asMemoryAddress
         )

   given [A]: Serializer[Ptr[A]] =
      ptrSerializer.asInstanceOf[Serializer[Ptr[A]]]

   def fromTypeInfo(
       a: Expr[?],
       memorySegment: Expr[MemorySegment],
       offset: Expr[Long],
       layout: Expr[MemoryLayout],
       path: Expr[Seq[PathElement]],
       typeInfo: TypeInfo
   )(using Quotes): Expr[Unit] =
      import quotes.reflect.*
      typeInfo match
         case PrimitiveInfo(name, '[a]) =>
            val to = Expr.summonOrError[Serializer[a]]
            '{
               $to.into(
                 ${ a.asExprOf[a] },
                 $memorySegment,
                 $layout.byteOffset($path*) + $offset
               )
            }
         case ProductInfo(name, members, '[a]) =>
            val aTerm = a.asTerm
            val aMembers =
               TypeRepr.of[a].typeSymbol.caseFields.map(s => s.name -> s).toMap
            val memberSelect = members.map { m =>
               val updatedPath = '{
                  $path :+ PathElement.groupElement(${ Expr(m.name) })
               }
               Select(aTerm, aMembers(m.name)).asExpr.pipe(
                 fromTypeInfo(_, memorySegment, offset, layout, updatedPath, m)
               )
            }
            Expr.block(memberSelect.toList, '{}).tap(_.show.tap(report.info))

         // pointer handling is exactly the same as primitive handling here.
         case PtrInfo(name, _, t) =>
            fromTypeInfo(
              a,
              memorySegment,
              offset,
              layout,
              path,
              PrimitiveInfo(name, t)
            )

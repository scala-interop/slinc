package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   SegmentAllocator,
   CLinker,
   MemoryAccess,
   MemorySegment,
   MemoryLayout,
   GroupLayout,
   MemoryAddress,
   ValueLayout
}, CLinker.C_INT, MemoryLayout.PathElement

import io.gitlab.mhammons.slinc.Ptr

import scala.quoted.*
import scala.util.chaining.*
import scala.jdk.CollectionConverters.*
import io.gitlab.mhammons.slinc.StaticArray

type Serializable[A] = Serializer[A] ?=> A
type SerializeFrom[A] = Serializer[A] ?=> Unit
object SerializeFrom:
   def apply[A](
       a: A,
       memoryAddress: MemoryAddress,
       offset: Long
   ): SerializeFrom[A] = summon[Serializer[A]].into(a, memoryAddress, offset)
//todo: rename to encoder
trait Serializer[A]:
   private def genPtr(
       memoryAddress: MemoryAddress,
       memoryLayout: MemoryLayout,
       offset: Long
   ): Ptr[Any] =
      memoryLayout match
         case gl: GroupLayout =>
            Ptr(
              memoryAddress,
              offset,
              gl.memberLayouts.asScala
                 .map(ml =>
                    ml.name.get.pipe(n =>
                       n -> genPtr(
                         memoryAddress,
                         ml,
                         gl.byteOffset(PathElement.groupElement(n))
                       )
                    )
                 )
                 .toMap
            )
         case vl: ValueLayout => Ptr(memoryAddress, offset)
   def to(a: A)(using
       layoutOf: NativeInfo[A]
   ): Allocates[Ptr[A]] =
      val segment =
         segAlloc.allocate(summon[NativeInfo[A]].layout)
      into(a, segment.address, 0)
      val l: MemoryLayout = summon[NativeInfo[A]].layout
      genPtr(segment.address, l, 0).asInstanceOf[Ptr[A]]
   def into(a: A, memoryAddress: MemoryAddress, offset: Long): Unit

object Serializer:
   given Serializer[Int] with
      def into(a: Int, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setIntAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Serializer[Long] with
      def into(a: Long, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setLongAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Serializer[Float] with
      def into(a: Float, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setFloatAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Serializer[Double] with
      def into(a: Double, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setDoubleAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Serializer[Short] with
      def into(a: Short, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setShortAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   given Serializer[Boolean] with
      def into(a: Boolean, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           if a then 1 else 0
         )

   given Serializer[Char] with
      def into(a: Char, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setCharAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )
   given Serializer[Byte] with
      def into(a: Byte, memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setByteAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           a
         )

   private val ptrSerializer = new Serializer[Ptr[Any]]:
      def into(ptr: Ptr[Any], memoryAddress: MemoryAddress, offset: Long) =
         MemoryAccess.setAddressAtOffset(
           MemorySegment.globalNativeSegment,
           memoryAddress.toRawLongValue + offset,
           ptr.asMemoryAddress
         )

   given [A]: Serializer[Ptr[A]] =
      ptrSerializer.asInstanceOf[Serializer[Ptr[A]]]

   given [A](using Serializer[A], NativeInfo[A]): Serializer[Array[A]] with
      def into(array: Array[A], memoryAddress: MemoryAddress, offset: Long) =
         var i = 0
         while i < array.length do
            SerializeFrom(
              array(i),
              memoryAddress,
              offset + (NativeInfo[A].layout.byteSize * i)
            )
            i += 1

   given [A, B <: Singleton & Int](using
       Serializer[A],
       NativeInfo[A],
       ValueOf[B]
   ): Serializer[StaticArray[A, B]] =
      new Serializer[StaticArray[A, B]]:
         def into(
             staticArray: StaticArray[A, B],
             memoryAddress: MemoryAddress,
             offset: Long
         ) =
            val nativeInfo = NativeInfo[A]
            val len = valueOf[B]
            var i = 0
            while i < staticArray.size do
               SerializeFrom(
                 staticArray(i),
                 memoryAddress,
                 offset + (nativeInfo.layout.byteSize * i)
               )
               i += 1

   def fromTypeInfo(
       a: Expr[?],
       memoryAddress: Expr[MemoryAddress],
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
                 $memoryAddress,
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
                 fromTypeInfo(_, memoryAddress, offset, layout, updatedPath, m)
               )
            }
            Expr.block(memberSelect.toList, '{}).tap(_.show.tap(report.info))

         // pointer handling is exactly the same as primitive handling here.
         case PtrInfo(name, _, t) =>
            fromTypeInfo(
              a,
              memoryAddress,
              offset,
              layout,
              path,
              PrimitiveInfo(name, t)
            )

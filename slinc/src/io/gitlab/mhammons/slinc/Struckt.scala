package io.gitlab.mhammons.slinc

import scala.compiletime.summonFrom
import io.gitlab.mhammons.slinc.components.NPtr
import scala.quoted.*
import jdk.incubator.foreign.{SegmentAllocator, MemorySegment}
import io.gitlab.mhammons.slinc.components.{LayoutOf, ToNative, FromNative}
import scala.CanEqual.derived
import components.summonOrError
import io.gitlab.mhammons.slinc.components.TypeInfo
import scala.deriving.Mirror
import io.gitlab.mhammons.slinc.components.BoundaryCrossing

trait Struckt[A <: Product]
    extends LayoutOf[A],
      ToNative[A],
      FromNative[A]
      
object Struckt:
   inline given derived[A <: Product]: Struckt[A] = ${
      derivedImpl[A]
   }


   private def derivedImpl[A <: Product: Type](using Quotes) =
      val typeInfo = TypeInfo[A]
      val mirror = Expr.summonOrError[Mirror.ProductOf[A]]

      '{
         new Struckt[A]:
            val carrierType = classOf[MemorySegment]
            val layout = ${ LayoutOf.fromTypeInfo(typeInfo) }
            def from(memorySegment: MemorySegment, offset: Long) =
               $mirror.fromProduct(${
                  FromNative.fromTypeInfo(
                    'memorySegment,
                    'layout,
                    'Nil,
                    typeInfo
                  )
               }.asInstanceOf[Product])

            def into(a: A, memorySegment: MemorySegment, offset: Long): Unit =
               ${
                  ToNative.fromTypeInfo(
                    'a,
                    'memorySegment,
                    'offset,
                    'layout,
                    'Nil,
                    typeInfo
                  )
               }
            def to(a: A)(using segAlloc: SegmentAllocator) =
               val segment = segAlloc.allocate(layout)
               into(a, segment, 0)
               NPtr[A](segment, 0)

            def toNative(a: A)(using SegmentAllocator) = to(a).asMemorySegment
      }

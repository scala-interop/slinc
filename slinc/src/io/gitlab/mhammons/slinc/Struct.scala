package io.gitlab.mhammons.slinc

import scala.compiletime.summonFrom
import scala.quoted.*
import jdk.incubator.foreign.{SegmentAllocator, MemorySegment}
import io.gitlab.mhammons.slinc.components.{LayoutOf, Serializer, Deserializer}
import scala.CanEqual.derived
import components.summonOrError
import io.gitlab.mhammons.slinc.components.TypeInfo
import scala.deriving.Mirror
import io.gitlab.mhammons.slinc.components.BoundaryCrossing
import scala.util.chaining.*

trait Struct[A <: Product] extends LayoutOf[A], Serializer[A], Deserializer[A]

object Struct:
   inline given derived[A <: Product]: Struct[A] = ${
      derivedImpl[A]
   }

   private def derivedImpl[A <: Product: Type](using Quotes) =
      import quotes.reflect.*
      val typeInfo = TypeInfo[A]
      val mirror = Expr.summonOrError[Mirror.ProductOf[A]]

      '{
         new Struct[A]:
            val carrierType = classOf[MemorySegment]
            val layout = ${ LayoutOf.fromTypeInfo(typeInfo) }
            def from(memorySegment: MemorySegment, offset: Long) =
               ${
                  Deserializer
                     .fromTypeInfo(
                       'memorySegment,
                       'layout,
                       Nil,
                       typeInfo
                     )
                     .asExpr
               }.asInstanceOf[A]

            def into(a: A, memorySegment: MemorySegment, offset: Long): Unit =
               ${
                  Serializer
                     .fromTypeInfo(
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
               Ptr[A](segment, 0)

            def toNative(a: A)(using SegmentAllocator) = to(a).asMemorySegment
      }

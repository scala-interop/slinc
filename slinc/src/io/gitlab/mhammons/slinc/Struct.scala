package io.gitlab.mhammons.slinc

import scala.compiletime.summonFrom
import scala.quoted.*
import jdk.incubator.foreign.{
   SegmentAllocator,
   MemoryAddress,
   MemorySegment,
   MemoryLayout
}
import io.gitlab.mhammons.slinc.components.{
   NativeInfo,
   Serializer,
   Deserializer
}
import scala.CanEqual.derived
import components.summonOrError
import io.gitlab.mhammons.slinc.components.{
   TypeInfo,
   PrimitiveInfo,
   ProductInfo,
   PtrInfo
}
import scala.deriving.Mirror
import io.gitlab.mhammons.slinc.components.BoundaryCrossing
import scala.util.chaining.*

trait Struct[A <: Product] extends NativeInfo[A], Serializer[A], Deserializer[A]

object Struct:
   inline given derived[A <: Product]: Struct[A] = ${
      derivedImpl[A]
   }

   private def derivedImpl[A <: Product: Type](using Quotes) =
      import quotes.reflect.*
      val typeInfo = TypeInfo[A]
      val mirror = Expr.summonOrError[Mirror.ProductOf[A]]

      '{
         // helps prevent recursion issues on Struct instantiation
         given l: NativeInfo[A] with
            val layout = ${ fromTypeInfo(typeInfo) }
            val carrierType = classOf[MemorySegment]

         new Struct[A]:
            val carrierType = classOf[MemorySegment]
            val layout = l.layout
            def from(memoryAddress: MemoryAddress, offset: Long) =
               ${
                  Deserializer
                     .fromTypeInfo(
                       'memoryAddress,
                       'layout,
                       Nil,
                       typeInfo
                     )
                     .asExpr
               }.asInstanceOf[A]

            def into(a: A, memoryAddress: MemoryAddress, offset: Long): Unit =
               ${
                  Serializer
                     .fromTypeInfo(
                       'a,
                       'memoryAddress,
                       'offset,
                       'layout,
                       Nil,
                       typeInfo
                     )
               }
      }

   def fromTypeInfo(typeInfo: TypeInfo)(using
       Quotes
   ): Expr[MemoryLayout] =
      typeInfo match
         case PrimitiveInfo(name, '[a]) =>
            '{
               ${ Expr.summonOrError[NativeInfo[a]] }.layout.withName(${
                  Expr(name)
               })
            }
         case PtrInfo(name, _, '[a]) =>
            '{
               ${ Expr.summonOrError[NativeInfo[a]] }.layout.withName(${
                  Expr(name)
               })
            }
         case ProductInfo(name, members, _) =>
            '{
               MemoryLayout
                  .structLayout(${
                     members.map(fromTypeInfo).pipe(Expr.ofSeq)
                  }*)
                  .withName(${ Expr(name) })
            }

// }
// def to(a: A)(using segAlloc: SegmentAllocator) =
//    val segment = segAlloc.allocate(layout)
//    into(a, segment.address, 0)
//    Ptr[A](segment.address, 0)

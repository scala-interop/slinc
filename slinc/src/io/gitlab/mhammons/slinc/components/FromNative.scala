package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryAccess, MemoryLayout},
MemoryLayout.PathElement
import scala.quoted.*
import scala.util.chaining.*
import scala.deriving.Mirror
//todo: rename to decoder
trait FromNative[A]:
   def from(memorySegment: MemorySegment, offset: Long): A

object FromNative:
   given FromNative[Int] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getIntAtOffset(memorySegment, offset)

   given FromNative[Float] with
      def from(memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.getFloatAtOffset(memorySegment, offset)

   def fromTypeInfo(
       memorySegmentExpr: Expr[MemorySegment],
       layout: Expr[MemoryLayout],
       path: Expr[Seq[PathElement]],
       typeInfo: TypeInfo
   )(using q: Quotes): Expr[?] =
      import quotes.reflect.report
      typeInfo match
         case PrimitiveInfo(name, '[a]) =>
            '{
               ${ Expr.summonOrError[FromNative[a]] }.from(
                 $memorySegmentExpr,
                 $layout.byteOffset(($path :+ PathElement.groupElement(${
                    Expr(name)
                 }))*)
               )
            }
         case ProductInfo(name, members, '[a]) =>
            val updatedPath =
               if name.isEmpty then path
               else '{ $path :+ PathElement.groupElement(${ Expr(name) }) }
            val mirror = Expr.summonOrError[Mirror.ProductOf[a]]
            '{
               $mirror.fromProduct(Tuple.fromArray(${
                  members
                     .map(m =>
                        fromTypeInfo(memorySegmentExpr, layout, updatedPath, m)
                     )
                     .pipe(Expr.ofSeq)
               }.toArray))
            }.tap(_.show.tap(report.warning))

         case PtrInfo(name, underlying, _) =>
            ???

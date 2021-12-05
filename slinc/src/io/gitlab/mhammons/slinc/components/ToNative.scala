package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{
   SegmentAllocator,
   CLinker,
   MemoryAccess,
   MemorySegment,
   MemoryLayout
}, CLinker.C_INT, MemoryLayout.PathElement

import scala.quoted.*
import scala.util.chaining.*

//todo: rename to encoder
trait ToNative[A]:
   def to(a: A)(using
       segAlloc: SegmentAllocator,
       layout: LayoutOf[A]
   ): NPtr[A] =
      val segment = segAlloc.allocate(layout.layout)
      into(a, segment, 0)
      NPtr[A](segment, 0)
   def into(a: A, memorySegment: MemorySegment, offset: Long): Unit

object ToNative:
   given ToNative[Int] with
      def into(a: Int, memorySegment: MemorySegment, offset: Long) =
         MemoryAccess.setIntAtOffset(memorySegment, offset, a)

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
            val to = Expr.summonOrError[ToNative[a]]
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

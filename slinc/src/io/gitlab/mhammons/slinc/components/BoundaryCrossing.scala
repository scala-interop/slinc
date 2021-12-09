package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import io.gitlab.mhammons.slinc.{Struct, Ptr}
import jdk.incubator.foreign.{
   SegmentAllocator,
   MemorySegment,
   CLinker,
   MemoryAddress
}

object BoundaryCrossing:
   def to[A: Type](a: Expr[A])(using Quotes): Expr[Any] =
      a match
         case '{ $b: Product & A } =>
            val struckt = Expr.summonOrError[Struct[Product & A]]
            val segAlloc = Expr.summonOrError[SegmentAllocator]

            '{
               $struckt.to($b)(using $segAlloc, $struckt).asMemorySegment
            }

         case '{ $b: Int }    => b
         case '{ $b: Long }   => b
         case '{ $b: Float }  => b
         case '{ $b: Double } => b
         case '{ $b: String } =>
            '{
               CLinker
                  .toCString($b, ${ Expr.summonOrError[SegmentAllocator] })
                  .address
            }

         case '{ $b: Ptr[a] } =>
            '{
               $b.asMemoryAddress
            }

   def from[A: Type](a: Expr[?])(using Quotes): Expr[A] =
      Type.of[A] match
         case '[Product & A] =>
            val struckt = Expr.summonOrError[Struct[Product & A]]
            '{
               $struckt.from($a.asInstanceOf[MemorySegment], 0)
            }
         case '[Int] | '[Long] | '[Float] | '[Double] =>
            '{ $a.asInstanceOf[A] }

         case '[Unit] => '{ () }.asExprOf[A]
         case '[String] =>
            '{ CLinker.toJavaString($a.asInstanceOf[MemoryAddress]) }
               .asExprOf[A]
         case '[Ptr[a]] =>
            val layoutOf = Expr.summonOrError[LayoutOf[a]]
            '{
               val address = $a.asInstanceOf[MemoryAddress]
               Ptr[a](
                 address.asSegment($layoutOf.layout.byteSize, address.scope),
                 0,
                 Map.empty
               )
            }.asExprOf[A]

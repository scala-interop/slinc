package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import io.gitlab.mhammons.slinc.Struckt
import jdk.incubator.foreign.{
   SegmentAllocator,
   MemorySegment,
   CLinker,
   MemoryAddress
}

object BoundaryCrossing2:
   def to[A: Type](a: Expr[A])(using Quotes): Expr[Any] =
      a match
         case '{ $b: Product & A } =>
            val struckt = Expr.summonOrError[Struckt[Product & A]]
            val segAlloc = Expr.summonOrError[SegmentAllocator]

            '{
               $struckt.to($b)(using $segAlloc).asMemorySegment
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

   def from[A: Type](a: Expr[?])(using Quotes): Expr[A] =
      Type.of[A] match
         case '[Product & A] =>
            val struckt = Expr.summonOrError[Struckt[Product & A]]
            '{
               $struckt.from($a.asInstanceOf[MemorySegment], 0)
            }
         case '[Int] | '[Long] | '[Float] | '[Double] =>
            '{ $a.asInstanceOf[A] }
         case '[String] =>
            '{ CLinker.toJavaString($a.asInstanceOf[MemoryAddress]) }
               .asExprOf[A]

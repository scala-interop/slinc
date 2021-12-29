package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import io.gitlab.mhammons.slinc.{Struct, Ptr}
import jdk.incubator.foreign.{
   SegmentAllocator,
   MemorySegment,
   CLinker,
   MemoryAddress
}
import io.gitlab.mhammons.slinc.StaticArray
import scala.reflect.ClassTag

object BoundaryCrossing:
   def to[A: Type](a: Expr[A])(using Quotes): Expr[Any] =
      a match
         case '{ $b: Product } =>
            val struckt = Expr.summonOrError[Struct[Product & A]]
            val segAlloc = '{ localAllocator }

            '{
               val segment = $segAlloc.allocate($struckt.layout)
               $struckt.into($b, segment.address, 0)
               segment
            }

         case '{ $b: Int }    => b
         case '{ $b: Long }   => b
         case '{ $b: Float }  => b
         case '{ $b: Double } => b
         case '{ $b: String } =>
            '{
               CLinker
                  .toCString($b, localAllocator)
                  .address
            }

         case '{ $b: Ptr[a] } =>
            '{
               $b.asMemoryAddress
            }

         case '{ $b: StaticArray[a, b] } =>
            val segAlloc = '{ localAllocator }
            val nativeInfo = Expr.summonOrError[NativeInfo[StaticArray[a, b]]]
            '{ $segAlloc.allocate($nativeInfo.layout) }

   def from[A: Type](a: Expr[?])(using Quotes): Expr[A] =
      Type.of[A] match
         case '[Product & A] =>
            val struckt = Expr.summonOrError[Struct[Product & A]]
            '{
               $struckt.from($a.asInstanceOf[MemorySegment].address, 0)
            }
         case '[Int] | '[Long] | '[Float] | '[Double] =>
            '{ $a.asInstanceOf[A] }

         case '[Unit] => '{ () }.asExprOf[A]
         case '[String] =>
            '{ CLinker.toJavaString($a.asInstanceOf[MemoryAddress]) }
               .asExprOf[A]
         case '[Ptr[a]] =>
            val layoutOf = Expr.summonOrError[NativeInfo[a]]
            val ct = Expr.summonOrError[ClassTag[a]]
            '{
               val address = $a.asInstanceOf[MemoryAddress]
               Ptr[a](
                 address,
                 0,
                 Map.empty
               )(using $ct)
            }.asExprOf[A]

         case '[StaticArray[a, b]] =>
            val layoutOf = Expr.summonOrError[NativeInfo[StaticArray[a, b]]]

            '{
               val address = $a.asInstanceOf[MemorySegment]
               ???
            }

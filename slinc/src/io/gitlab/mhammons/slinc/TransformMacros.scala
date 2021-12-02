package io.gitlab.mhammons.slinc

import scala.quoted.*
import jdk.incubator.foreign.{
   MemoryLayout,
   CLinker,
   MemoryAddress,
   MemorySegment,
   SegmentAllocator
}
import scala.compiletime.summonInline
import CLinker.{C_CHAR, C_FLOAT, C_DOUBLE, C_POINTER, C_LONG, C_SHORT, C_INT}
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import scala.util.chaining.*
import io.gitlab.mhammons.slinc.components.{MemLayout, Member}

object TransformMacros:
   def type2MemLayout[A: Type](using Quotes): Expr[MemLayout] =
      import components.{Primitives, Str}
      import quotes.reflect.report
      Type.of[A] match
         case '[Int]    => '{ Primitives.Int }
         case '[Float]  => '{ Primitives.Float }
         case '[Long]   => '{ Primitives.Long }
         case '[String] => '{ Str }
         case '[Double] => '{ Primitives.Double }
         case '[Struct] =>
            '{
                  LayoutMacros.deriveLayout2[A]
            }
         case '[Member[t]] => type2MemLayout[t]
         case '[t] => report.errorAndAbort(s"Unrecognized type ${Type.show[t]}")


   def type2MethodTypeArg[A: Type](using Quotes): Expr[Class[?]] =
      import quotes.reflect.*
      Type.of[A] match
         case '[Long]      => '{ classOf[Long] }
         case '[Int]       => '{ classOf[Int] }
         case '[Float]     => '{ classOf[Float] }
         case '[Double]    => '{ classOf[Double] }
         case '[Boolean]   => '{ classOf[Boolean] }
         case '[Char]      => '{ classOf[Char] }
         case '[String]    => '{ classOf[MemoryAddress] }
         case '[Short]     => '{ classOf[Short] }
         case '[Struct]    => '{ classOf[MemorySegment] }
         case '[Member[t]] => type2MethodTypeArg[t]
         case '[a] =>
            report.errorAndAbort(s"received unknown type ${Type.show[a]}")

   type scala2Native[S] = S match
      case Struct => MemorySegment
      case String => MemoryAddress
      case _      => S

   def paramModifiers(
       segAlloc: Option[Expr[SegmentAllocator]]
   )(param: Type[?])(using Quotes) =
      param match
         case '[String] =>
            val impl = segAlloc.get
            (a: Expr[Any]) =>
               '{
                  CLinker
                     .toCString(
                       ${ a.asExprOf[String] },
                       $impl
                     )
                     .address
               }
         case '[Struct] =>
            (a: Expr[Any]) =>
               '{
                  ${ a.asExprOf[Struct] }.$mem
               }
         case _ => (a: Expr[Any]) => a


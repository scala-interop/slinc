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
import io.gitlab.mhammons.slinc.components.MemLayout

object TransformMacros:
   def type2MemLayout[A: Type](using Quotes): Expr[MemLayout] =
      import components.Primitives
      import quotes.reflect.report
      Type.of[A] match
         case '[Int]   => '{ Primitives.Int }
         case '[Float] => '{ Primitives.Float }
         case '[Long]  => '{ Primitives.Long }
         case '[Struct] =>
            '{
               ${ Expr.summon[NativeCache].getOrElse(missingNativeCache) }
                  .layout[A]
            }
         case '[Member[t]] => type2MemLayout[t]

   // def type2MemoryLayout[A: Type](using
   //     q: Quotes
   // ): Expr[MemoryLayout] =
   //    Type.of[A] match
   //       case '[Int]       => '{ C_INT }
   //       case '[Float]     => '{ C_FLOAT }
   //       case '[Double]    => '{ C_DOUBLE }
   //       case '[Boolean]   => '{ C_CHAR }
   //       case '[Char]      => '{ C_CHAR }
   //       case '[String]    => '{ C_POINTER }
   //       case '[Short]     => '{ C_SHORT }
   //       case '[Long]      => '{ C_LONG }
   //       case '[Member[a]] => type2MemoryLayout[a]
   //       case '[Struct]    => '{ summonInline[NativeCache].layout[A] }

   // inline def type2MemLayout[A] =
   //    inline erasedValue match
   //       case _: Int     => C_INT
   //       case _: Float   => C_FLOAT
   //       case _: Double  => C_DOUBLE
   //       case _: Boolean => C_CHAR
   //       case _: Char    => C_CHAR
   //       case _: String  => C_POINTER
   //       case _: Short   => C_SHORT
   //       case _: Long    => C_LONG
   //       case _: Struct  => summonInline[NativeCache].layout[A]

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

   def param2Native[T: Type](p: Expr[T])(using Quotes) =
      import quotes.reflect.report
      p match
         case '{ $string: String } =>
            val alloc = Expr
               .summon[SegmentAllocator]
               .getOrElse(
                 report.errorAndAbort(
                   "This binding needs a segment allocator. Please import one or make one available in your scope"
                 )
               )
            '{
               CLinker
                  .toCString($string, $alloc)
                  .address
            }
         case '{ $struct: Struct } =>
            '{ $struct.$mem }
         case _ => p

   def native2ST[ST: Type](n: Expr[Any])(using Quotes) =
      import quotes.reflect.report
      (n, Type.of[ST]) match
         case ('{ $memSgmnt: MemorySegment }, '[Struct]) =>
            val nCache = Expr
               .summon[NativeCache]
               .getOrElse(
                 report.errorAndAbort(
                   "A native cache is needed for this function to operate. Please import one from slinc.NativeCache"
                 )
               )
            '{
               val sgmnt = $memSgmnt
               Struct(
                 $nCache
                    .varHandles[ST]
                    .map((name, vh) => name -> Member(sgmnt, vh))
                    .toMap
                    .updated("$mem", sgmnt),
                 sgmnt
               ).asInstanceOf[ST]
            }
         case ('{ $i: Int }, '[Int])       => i.asExprOf[ST]
         case ('{ $l: Long }, '[Long])     => l.asExprOf[ST]
         case ('{ $d: Double }, '[Double]) => d.asExprOf[ST]
         case ('{ $s: MemoryAddress }, '[String]) =>
            val nCache = Expr.summon[NativeCache].getOrElse(missingNativeCache)
            '{
               val addr = $s
               CLinker.toJavaString(addr)
            }.asExprOf[ST]
         case (expr, _) =>
            report.errorAndAbort(
              s"got expr ${expr.show} with conversion to ${Type.show[ST]} requested"
            )

package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{
   FunctionDescriptor,
   CLinker,
   SegmentAllocator,
   MemoryAddress,
   MemoryLayout
}
import io.gitlab.mhammons.polymorphics.{MethodHandleHandler, VoidHelper}
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

object MethodHandleMacros:
   inline def methodTypeForFn[A](using Fn[A]) = ${
      methodTypeForFnImpl[A]
   }

   private def methodTypeForFnImpl[A: Type](using q: Quotes): Expr[MethodType] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(trepr, args)
             if trepr.typeSymbol.name.startsWith("Function") =>
            val types = args.map(_.asType)
            val inputLayouts = types.init.map { case '[a] =>
               '{ ${ Expr.summonOrError[NativeInfo[a]] }.carrierType }
            }
            val result = types.last.pipe { case '[r] =>
               Expr.summon[NativeInfo[r]].map(ni => '{ $ni.carrierType })
            }
            methodType(result, inputLayouts)

   inline def functionDescriptorForFn[A](using Fn[A]): FunctionDescriptor =
      ${
         functionDescriptorForFnImpl[A]
      }

   private def functionDescriptorForFnImpl[A](using q: Quotes)(using
       Type[A]
   ): Expr[FunctionDescriptor] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(_, args) =>
            val types = args.map(_.asType)
            val inputs = types.init.map { case '[a] =>
               '{ ${ Expr.summonOrError[NativeInfo[a]] }.layout }
            }
            val retType = types.last.pipe { case '[r] =>
               Expr.summon[NativeInfo[r]].map(e => '{ $e.layout })
            }

            functionDescriptor(retType, inputs)

   def methodType(
       returnCarrierType: Option[Expr[Class[?]]],
       carrierTypes: List[Expr[Class[?]]]
   )(using Quotes) =
      import java.lang.invoke.MethodType

      returnCarrierType
         .map { r =>
            if carrierTypes.isEmpty then '{ MethodType.methodType($r) }
            else
               '{
                  MethodType.methodType(
                    $r,
                    ${ carrierTypes.head },
                    ${ Varargs(carrierTypes.tail) }*
                  )
               }
         }
         .getOrElse {
            if carrierTypes.isEmpty then '{ VoidHelper.methodTypeV() }
            else
               '{
                  VoidHelper.methodTypeV(
                    ${ carrierTypes.head },
                    ${ Varargs(carrierTypes.tail) }*
                  )
               }
         }

   private def functionDescriptor(
       returnLayout: Option[Expr[MemoryLayout]],
       paramLayouts: List[Expr[MemoryLayout]]
   )(using Quotes): Expr[FunctionDescriptor] =
      returnLayout
         .map { r =>
            '{ FunctionDescriptor.of($r, ${ Varargs(paramLayouts) }*) }
         }
         .getOrElse('{ FunctionDescriptor.ofVoid(${ Varargs(paramLayouts) }*) })

   private def paramsToLayoutAndCarrier(params: List[Expr[Any]])(using
       Quotes
   ): (List[Expr[MemoryLayout]], List[Expr[Class[?]]]) =
      params
         .map(_.widen)
         .flatMap { case '{ $z: a } =>
            val a = Expr.summonOrError[NativeInfo[a]]
            Seq('{ $a.layout } -> '{ $a.carrierType })
         }
         .unzip

   private def dc[Ret](
       addr: Expr[MemoryAddress],
       params: List[Expr[Any]]
   )(using Quotes, Type[Ret]): Expr[Allocatee[MethodHandle]] =
      val (returnLayout, returnCarrierType) = Type.of[Ret] match
         case '[Unit] => (None, None)
         case '[r] =>
            Expr
               .summonOrError[NativeInfo[r]]
               .pipe(exp =>
                  Some('{ $exp.layout }) -> Some('{ $exp.carrierType })
               )

      val (layouts, carrierTypes) = paramsToLayoutAndCarrier(params)
      val functionD = functionDescriptor(returnLayout, layouts)
      val methodT = methodType(returnCarrierType, carrierTypes)

      '{
         Linker.linker.downcallHandle($addr, segAlloc, $methodT, $functionD)
      }

   def call[Ret: Type](
       mh: Expr[MethodHandle],
       ps: List[Expr[Any]]
   )(using
       Quotes
   ): Expr[Ret] =
      import quotes.reflect.*

      def callFn(mh: Expr[MethodHandle], args: List[Expr[Any]]) = Apply(
        Ident(
          TermRef(TypeRepr.of[MethodHandleHandler.type], s"call${ps.size}")
        ),
        mh.asTerm +: ps.map(_.asTerm)
      ).asExprOf[Any]

      Expr.betaReduce('{
         given Immigrator[Ret] = ${
            Expr.summonOrError[Immigrator[Ret]]
         }
         immigrator[Ret](${ callFn(mh, ps) })
      })

   def binding[Ret: Type](
       address: Expr[MemoryAddress],
       ps: List[Expr[Any]]
   )(using
       Quotes
   ) =
      import quotes.reflect.*
      val methodHandle =
         dc[Ret](
           address,
           ps
         )

      Expr.betaReduce('{
         given SegmentAllocator = localAllocator
         try {
            val mh = $methodHandle
            ${
               call(
                 '{ mh },
                 ps.map(_.widen).map { case '{ $a: a } =>
                    '{ ${ Expr.summonOrError[Emigrator[a]] }($a) }
                 }
               )
            }
         } finally {
            reset()
         }

      })

end MethodHandleMacros

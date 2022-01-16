package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{
   FunctionDescriptor,
   CLinker,
   SegmentAllocator,
   MemoryAddress
}
import io.gitlab.mhammons.polymorphics.{MethodHandleHandler, VoidHelper}
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

object MethodHandleMacros:
   // TODO: use erased instead of using here
   inline def methodTypeForFn[A](using Fn[A]) = ${
      methodTypeForFnImpl[A]
   }

   private def methodTypeForFnImpl[A: Type](using Quotes): Expr[MethodType] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(trepr, args)
             if trepr.typeSymbol.name.startsWith("Function") =>
            val types = args.map(_.asType)
            val inputTypes = types.init
            val result = types.last
            result.pipe { case '[r] => methodType[r](inputTypes) }

   def methodType[Ret: Type](args: List[Type[?]])(using Quotes) =
      import java.lang.invoke.MethodType

      val methodTypes = args.map { case '[p] =>
         Expr
            .summonOrError[NativeInfo[p]]
            .pipe(exp => '{ $exp.carrierType })
      }
      Type.of[Ret] match
         case '[Unit] =>
            if args.isEmpty then '{ VoidHelper.methodTypeV() }
            else
               '{
                  VoidHelper.methodTypeV(
                    ${ methodTypes.head },
                    ${ Varargs(methodTypes.tail) }*
                  )
               }
         case '[r] =>
            val returnMethodType = Expr
               .summonOrError[NativeInfo[Ret]]
               .pipe(exp => '{ $exp.carrierType })
            if args.isEmpty then '{ MethodType.methodType($returnMethodType) }
            else
               '{
                  MethodType.methodType(
                    $returnMethodType,
                    ${ methodTypes.head },
                    ${ Varargs(methodTypes.tail) }*
                  )
               }

   inline def functionDescriptorForFn[A](using Fn[A]): FunctionDescriptor = ${
      functionDescriptorForFnImpl[A]
   }

   private def functionDescriptorForFnImpl[A](using
       Quotes,
       Type[A]
   ): Expr[FunctionDescriptor] =
      import quotes.reflect.*

      TypeRepr.of[A] match
         case AppliedType(_, args) =>
            val types = args.map(_.asType)
            val inputs = types.init
            val retType = types.last

            retType.pipe { case '[r] => functionDescriptor[r](inputs) }

   def functionDescriptor[Ret: Type](paramTypes: List[Type[?]])(using
       Quotes
   ): Expr[FunctionDescriptor] =
      val paramLayouts =
         paramTypes
            .map { case '[p] =>
               Expr
                  .summonOrError[NativeInfo[p]]
                  .pipe(exp => '{ $exp.layout })
            }
            .pipe(Varargs.apply)

      Type.of[Ret] match
         case '[Unit] =>
            '{ FunctionDescriptor.ofVoid($paramLayouts*) }
         case '[r] =>
            '{
               FunctionDescriptor.of(
                 ${
                    Expr.summonOrError[NativeInfo[r]]
                 }.layout,
                 $paramLayouts*
               )
            }

   def downcall[Ret: Type](
       name: String,
       params: List[Type[?]]
   )(using Quotes): Expr[Allocatee[MethodHandle]] =
      val functionD = functionDescriptor[Ret](params)
      val methodT = methodType[Ret](params)

      val symbolLookup = Expr.summonOrError[SymbolLookup]
      val nameExpr = Expr(name)

      '{
         Linker.linker.downcallHandle(
           $symbolLookup.lookup($nameExpr),
           segAlloc,
           $methodT,
           $functionD
         )
      }

   def downcall[Ret](addr: Expr[MemoryAddress], params: List[Type[?]])(using
       Quotes,
       Type[Ret]
   ): Expr[Allocatee[MethodHandle]] =
      val functionD = functionDescriptor[Ret](params)
      val methodT = methodType[Ret](params)

      '{
         Linker.linker.downcallHandle(
           $addr,
           segAlloc,
           $methodT,
           $functionD
         )
      }

   def call[Ret: Type](mh: Expr[MethodHandle], ps: List[Expr[Any]])(using
       Quotes
   ): Expr[Immigratee[Ret, Allocatee[Ret]]] =
      import quotes.reflect.*

      def callFn(mh: Expr[MethodHandle], args: List[Expr[Any]]) = Apply(
        Ident(
          TermRef(TypeRepr.of[MethodHandleHandler.type], s"call${ps.size}")
        ),
        mh.asTerm :: ps.map(_.asTerm)
      ).asExprOf[Any]

      '{
         immigrator[Ret](${ callFn(mh, ps) })
      }

   def binding[Ret: Type](
       address: Expr[MemoryAddress],
       ps: List[(Expr[Any], Type[?])]
   )(using
       Quotes
   ) =
      import quotes.reflect.*
      val methodHandle =
         downcall[Ret](
           address,
           ps.map { case (_, '[a]) =>
              Type.of[a]
           }
         )

      '{
         given Immigrator[Ret] = ${
            Expr.summonOrError[Immigrator[Ret]]
         }
         given SegmentAllocator = localAllocator
         try {
            val mh = $methodHandle
            ${
               call(
                 '{ mh },
                 ps.map { case (a, '[b]) =>
                    '{
                       ${ Expr.summonOrError[Emigrator[b]] }(${
                          a.asExprOf[b]
                       })
                    }
                 }
               )
            }
         } finally {
            reset()
         }

      }

end MethodHandleMacros

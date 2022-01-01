package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{FunctionDescriptor, CLinker}
import scala.language.experimental

object MethodHandleMacros:
   def methodType[Ret: Type](args: List[Type[?]])(using Quotes) =
      import java.lang.invoke.MethodType

      val methodTypes = args.map { case '[p] =>
         Expr
            .summon[NativeInfo[p]]
            .getOrElse(missingLayout[p])
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
   def functionDescriptor[Ret: Type](paramTypes: List[Type[?]])(using
       Quotes
   ): Expr[FunctionDescriptor] =
      val paramLayouts =
         paramTypes
            .map { case '[p] =>
               Expr
                  .summon[NativeInfo[p]]
                  .getOrElse(missingLayout[p])
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
   )(using Quotes) =
      val functionD = functionDescriptor[Ret](params)
      val methodT = methodType[Ret](params)

      val symbolLookup = Expr.summon[SymbolLookup].getOrElse(???)
      val nameExpr = Expr(name)

      val idx = Expr(UniversalNativeCache.getBindingIndex(name))
      '{
         UniversalNativeCache.addMethodHandle(
           $idx,
           Linker.linker.downcallHandle(
             $symbolLookup.lookup($nameExpr),
             $methodT,
             $functionD
           )
         )
      }
end MethodHandleMacros

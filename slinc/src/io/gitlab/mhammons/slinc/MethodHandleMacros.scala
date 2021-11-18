package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{FunctionDescriptor, CLinker}
import components.SymbolLookup
import scala.compiletime.summonInline

object MethodHandleMacros:
   import TransformMacros.{type2MethodTypeArg, type2MemLayout}
   def methodType[Ret: Type](args: List[Type[?]])(using Quotes) =
      import java.lang.invoke.MethodType

      val methodTypes = args.map { case '[p] => type2MethodTypeArg[p] }
      val returnMethodType = type2MethodTypeArg[Ret]
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
            .map { case '[p] => '{ ${ type2MemLayout[p] }.underlying } }
            .pipe(Varargs.apply)

      Type.of[Ret] match
         case '[Unit] =>
            '{ FunctionDescriptor.ofVoid($paramLayouts*) }
         case '[r] =>
            '{
               FunctionDescriptor.of(
                 ${ type2MemLayout[r] }.underlying,
                 $paramLayouts*
               )
            }

   def downcall[Ret: Type](
       name: Expr[String],
       params: List[Type[?]]
   )(using Quotes) =
      val functionD = functionDescriptor[Ret](params)
      val methodT = methodType[Ret](params)

      val nCache = Expr.summon[NativeCache].getOrElse(missingNativeCache)
      val symbolLookup = Expr.summon[SymbolLookup].getOrElse(???)
      '{
         val c = $nCache
         c.downcall(
           $name,
           c.clinker
              .downcallHandle($symbolLookup.lookup($name), $methodT, $functionD)
         )
      }
end MethodHandleMacros

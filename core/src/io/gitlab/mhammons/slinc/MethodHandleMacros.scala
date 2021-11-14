package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.{FunctionDescriptor, CLinker}
import scala.compiletime.summonInline

object MethodHandleMacros:
   import TransformMacros.{type2MethodTypeArg, type2MemoryLayout}
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
            .map { case '[p] => type2MemoryLayout[p] }
            .pipe(Varargs.apply)

      Type.of[Ret] match
         case '[Unit] =>
            '{ FunctionDescriptor.ofVoid($paramLayouts*) }
         case '[r] =>
            '{
               FunctionDescriptor.of(${ type2MemoryLayout[r] }, $paramLayouts*)
            }

   def downcall[Ret: Type](
       name: Expr[String],
       params: List[Type[?]]
   )(using Quotes) =
      val functionD = functionDescriptor[Ret](params)
      val methodT = methodType[Ret](params)

      '{
         val c = summonInline[NativeCache]
         c.downcall(
           $name,
           c.clinker.downcallHandle(clookup($name), $methodT, $functionD)
         )
      }
end MethodHandleMacros

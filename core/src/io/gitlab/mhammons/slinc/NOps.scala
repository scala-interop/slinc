package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import io.gitlab.mhammons.polymorphics.MethodHandleHandler
import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.SegmentAllocator
import scala.util.{Try => T}
import jdk.incubator.foreign.ResourceScope

transparent inline def bind = ${
   bindImpl
}

inline def bind2[Ret](a: Any) = ${
   bindImpl2[Ret]('a)
}

private def bindImpl2[Ret: Type](expr: Expr[Any])(using Quotes) =
   import quotes.reflect.*

   val ownersOwner = Symbol.spliceOwner.owner.owner.tree

   '{}

private def needsAllocator[T: Type](returnType: Boolean)(using Quotes) =
   Type.of[T] match
      case '[Struct] => returnType
      case '[String] => !returnType
      case _         => false

private def bindImpl(using q: Quotes) =
   import quotes.reflect.*

   val owner = Symbol.spliceOwner.owner

   if owner.isDefDef then

      T(owner.tree).fold(
        _ =>
           report.errorAndAbort(
             "Could not properly analyze this method definition because the return type is missing. Please add one"
           ),
        identity
      ) match
         case d @ DefDef(name, parameters, dt, _) =>
            val mhgen = parameters
               .collectFirst {
                  case TypeParamClause(_) =>
                     report.errorAndAbort(
                       "Cannot generate C bind from generic method"
                     )
                  case t @ TermParamClause(valDefs) =>
                     dt.tpe.asType match
                        case '[ret] =>
                           val params = valDefs.map(t => Ref(t.symbol).asExpr)

                           val mh = MethodHandleMacros.downcall[ret](
                             Expr(name),
                             params.map { case '{ $x: p } => Type.of[p] }
                           )

                           val resultTypeAllocates =
                              needsAllocator[ret](true)

                           val segAllocArg =
                              if resultTypeAllocates then
                                 List(
                                   Expr
                                      .summon[SegmentAllocator]
                                      .getOrElse(
                                        report.errorAndAbort(
                                          s"This method binding needs a segment allocator. Please redefine your method $name with a (using SegmentAllocator) clause"
                                        )
                                      )
                                      .asExprOf[Any]
                                 )
                              else Nil

                           val transformedParams = params
                              .map { case '{ $x: p } =>
                                 TransformMacros.param2Native[p](x)
                              }
                              .pipe(segAllocArg ++ _)

                           call[ret](mh, transformedParams)

               }
               .getOrElse(
                 dt.tpe.asType match
                    case '[ret] =>
                       val mh =
                          MethodHandleMacros.downcall[ret](Expr(name), Nil)
                       val resultTypeAllocates = needsAllocator[ret](true)
                       val segAllocArg =
                          if resultTypeAllocates then
                             List(
                               Expr
                                  .summon[SegmentAllocator]
                                  .getOrElse(
                                    report.errorAndAbort(
                                      s"This method binding needs a segment allocator. Please redefine your method $name with a (using SegmentAllocator) clause"
                                    )
                                  )
                                  .asExprOf[Any]
                             )
                          else Nil

                       call[ret](mh, segAllocArg)
               ) // MethodHandleMacros.downcall[Ret](Expr(name), Nil))

            mhgen.tap(_.show.tap(report.info))
   else report.errorAndAbort("didn't get defdef")
end bindImpl

def scope[A](fn: (SegmentAllocator) ?=> A) =
   val resourceScope = ResourceScope.newConfinedScope
   given SegmentAllocator = SegmentAllocator.arenaAllocator(resourceScope)
   T(fn).fold(
     t => throw t.tap(_ => resourceScope.close),
     _.tap(_ => resourceScope.close)
   )

def lazyScope[A](fn: (SegmentAllocator) ?=> A) =
   val resourceScope = ResourceScope.newImplicitScope
   given SegmentAllocator = SegmentAllocator.arenaAllocator(resourceScope)
   fn


def call[Ret: Type](mh: Expr[MethodHandle], ps: List[Expr[Any]])(using
    Quotes
): Expr[Ret] =
   import quotes.reflect.*

   def callFn(mh: Expr[MethodHandle], args: List[Expr[Any]]) = Apply(
     Ident(TermRef(TypeRepr.of[MethodHandleHandler.type], s"call${ps.size}")),
     mh.asTerm :: ps.map(_.asTerm)
   ).asExprOf[Any]

   '{
      val methodHandle = $mh
      ${
         TransformMacros.native2ST[Ret](
           '{
              ${ callFn('methodHandle, ps) }
                 .asInstanceOf[TransformMacros.scala2Native[Ret]]
           }
         )
      }
   }

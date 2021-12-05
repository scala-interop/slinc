package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import io.gitlab.mhammons.polymorphics.MethodHandleHandler
import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.SegmentAllocator
import scala.util.{Try => T}
import jdk.incubator.foreign.ResourceScope
import scala.compiletime.summonInline
import io.gitlab.mhammons.slinc.components.{
   Member,
   Template,
   Allocatable,
   BoundaryCrossing,
   missingBoundaryCrossing,
   missingSegmentAllocator
}
import io.gitlab.mhammons.slinc.components.{SegmentTemplate, summonOrError}
import io.gitlab.mhammons.slinc.components.BoundaryCrossing2

transparent inline def bind = ${
   bindImpl
}

private def needsAllocator[T: Type](returnType: Boolean)(using Quotes) =
   Type.of[T] match
      case '[Struct]  => returnType
      case '[Product] => returnType
      case '[String]  => !returnType
      case _          => false

private def bindImpl(using q: Quotes) =
   import quotes.reflect.*

   val owner = Symbol.spliceOwner.owner

   val (name, params, ret) =
      if owner.isDefDef then

         T(owner.tree).fold(
           _ =>
              report.errorAndAbort(
                "Could not properly analyze this method definition because the return type is missing. Please add one"
              ),
           identity
         ) match
            case d @ DefDef(name, parameters, dt, _) =>
               parameters
                  .collectFirst {
                     case TypeParamClause(_) =>
                        report.errorAndAbort(
                          "Cannot generate C bind from generic method"
                        )
                     case t @ TermParamClause(valDefs) =>
                        val params = valDefs.map(t =>
                           Ref(t.symbol).asExpr -> t.tpt.tpe.asType
                        )
                        (name, params, dt.tpe.asType)
                  }
                  .getOrElse(
                    (name, Nil, dt.tpe.asType)
                  )
      else report.errorAndAbort("didn't get defdef")

   val segAllocArg =
      if ret.pipe { case '[r] => needsAllocator[r](true) } then
         List(Expr.summon[SegmentAllocator].getOrElse(missingSegmentAllocator))
      else Nil

   val methodHandle = ret.pipe { case '[r] =>
      components.MethodHandleMacros.downcall[r](name, params.map(_._2))
   }

   val callFn = ret.pipe { case '[r] => call[r](methodHandle, _) }

   params
      .map { case (expr, '[a]) =>
         val aExpr = expr.asExprOf[a]
         // Expr
         //    .summonOrError[BoundaryCrossing[a, ?]]
         //    .pipe(bc => '{ $bc.toNative($aExpr) })
         BoundaryCrossing2.to(aExpr)
      }
      .pipe(segAllocArg ++ _)
      .pipe(callFn(_))
      .tap(_.show.tap(report.info))
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

   BoundaryCrossing2.from[Ret](callFn(mh, ps)).tap(_.show.tap(report.warning))

def allocate[A: SegmentTemplate](using
    seg: SegmentAllocator,
    template: SegmentTemplate[A]
) =
   template(seg.allocate(template.layout))

type int = Member[Int]
type float = Member[Float]
type long = Member[Long]

extension [A <: Product](a: A)(using struckt: Struckt[A])
   def serialize(using SegmentAllocator) = struckt.to(a)

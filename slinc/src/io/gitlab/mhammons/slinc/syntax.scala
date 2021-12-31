package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import io.gitlab.mhammons.polymorphics.MethodHandleHandler
import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.SegmentAllocator
import scala.util.{Try => T}
import jdk.incubator.foreign.ResourceScope
import io.gitlab.mhammons.slinc.components.{
   summonOrError,
   BoundaryCrossing,
   Serializer,
   NativeInfo,
   localAllocator,
   Allocates,
   segAlloc
}
import scala.reflect.ClassTag

transparent inline def bind = ${
   bindImpl
}

private def needsAllocator[T: Type](returnType: Boolean)(using Quotes) =
   Type.of[T] match
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
         List('{ localAllocator(0) })
      else Nil

   val methodHandle = ret.pipe { case '[r] =>
      components.MethodHandleMacros.downcall[r](name, params.map(_._2))
   }

   val callFn = ret.pipe { case '[r] => call[r](methodHandle, _) }

   params
      .map { case (expr, '[a]) =>
         val aExpr = expr.asExprOf[a]
         BoundaryCrossing.to(aExpr)
      }
      .pipe(segAllocArg ++ _)
      .pipe(callFn(_))
end bindImpl

type Allocatable[A] = SegmentAllocator ?=> A
type Quoted[A] = Quotes ?=> Expr[A]

def scope[A](fn: ResourceScope ?=> (SegmentAllocator) ?=> A) =
   given resourceScope: ResourceScope = ResourceScope.newConfinedScope
   given SegmentAllocator = SegmentAllocator.arenaAllocator(resourceScope)
   try {
      fn
   } finally {
      resourceScope.close
   }

def allocScope[A](fn: ResourceScope ?=> SegmentAllocator ?=> A) =
   given resourceScope: ResourceScope = ResourceScope.newConfinedScope
   given SegmentAllocator = SegmentAllocator.ofScope(resourceScope)
   try {
      fn
   } finally {
      resourceScope.close
   }

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

   BoundaryCrossing.from[Ret](callFn(mh, ps))

extension [A](a: A)(using to: Serializer[A], layoutOf: NativeInfo[A])
   def serialize(using SegmentAllocator) = to.to(a)

extension [A: ClassTag](
    a: Array[A]
)(using to: Serializer[A], layoutOf: NativeInfo[A])
   def serialize: Allocates[Ptr[A]] =
      val addr = segAlloc.allocateArray(layoutOf.layout, a.size).address
      var i = 0
      while i < a.length do
         to.into(a(i), addr, i * layoutOf.layout.byteSize)
         i += 1
      Ptr[A](addr, 0, Map.empty)

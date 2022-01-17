package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import scala.util.{Try => T}

import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.ResourceScope
import io.gitlab.mhammons.slinc.components.{
   Allocatee,
   MethodHandleMacros,
   Informee,
   Serializee,
   Exportee,
   Scopee,
   SymbolLookup,
   segAlloc,
   serializerOf,
   infoOf,
   exportValue,
   summonOrError,
   selectPlatform,
   Platform
}
import scala.reflect.ClassTag
import io.gitlab.mhammons.slinc.components.PlatformX64Linux

transparent inline def bind = ${
   bindImpl('false)
}
transparent inline def bind(debug: Boolean = false) = ${
   bindImpl('debug)
}

private def bindImpl(debugExpr: Expr[Boolean])(using q: Quotes) =
   import quotes.reflect.*

   val debug = debugExpr.valueOrAbort

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
                           t.tpt.tpe.asType.pipe { case '[p] =>
                              Ref(t.symbol).asExprOf[p]
                           } -> t.tpt.tpe.asType
                        )
                        (name, params, dt.tpe.asType)
                  }
                  .getOrElse(
                    (name, Nil, dt.tpe.asType)
                  )
      else report.errorAndAbort("didn't get defdef")

   val memoryAddress = '{
      val symbolLookup = ${ Expr.summonOrError[SymbolLookup] }
      val fnName = ${ Expr(name) }
      symbolLookup.lookup(fnName)
   }

   ret.pipe { case '[r] =>
      MethodHandleMacros.binding[r](memoryAddress, params)
   }.tap { exp =>
      if debug then report.info(exp.show)
   }

end bindImpl

def scope[A](fn: ResourceScope ?=> Allocatee[A]) =
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

extension [A](a: A)
   def serialize: Allocatee[Scopee[Informee[A, Exportee[A, Ptr[A]]]]] =
      val addr = exportValue(a)
      Ptr[A](addr, 0)

extension [A: ClassTag](
    a: Array[A]
)
   def serialize: Allocatee[Scopee[Informee[A, Exportee[Array[A], Ptr[A]]]]] =
      val addr = exportValue(a)
      Ptr[A](addr, 0)

extension [A, S <: Iterable[A]](s: S)
   def serialize: Informee[A, Serializee[A, Allocatee[Ptr[A]]]] =
      val addr = segAlloc.allocateArray(infoOf[A].layout, s.size).address
      s.zipWithIndex.foreach((a, i) =>
         serializerOf[A].into(a, addr, i * infoOf[A].layout.byteSize)
      )
      Ptr[A](addr, 0)

val platform: Platform = selectPlatform

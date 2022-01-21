package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import scala.reflect.ClassTag
import scala.util.NotGiven
import scala.annotation.implicitNotFound

import jdk.incubator.foreign.{SegmentAllocator, ResourceScope, CLinker}

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
   layoutOf,
   exportValue,
   summonOrError,
   Platform,
   widen
}

inline def bind[R](using
    @implicitNotFound(
      "You must provide a return type for bind"
    ) n: NotGiven[R =:= Nothing]
): R = ${
   bindImpl[R]('false)
}
inline def bind[R](debug: Boolean = false)(using
    @implicitNotFound(
      "You must provide a return type for bind"
    ) n: NotGiven[R =:= Nothing]
): R = ${
   bindImpl[R]('debug)
}

private def bindImpl[R](debugExpr: Expr[Boolean])(using q: Quotes)(using
    Type[R]
): Expr[R] =
   import quotes.reflect.*

   val debug = debugExpr.valueOrAbort

   val owner = Symbol.spliceOwner.owner

   val (name, params) =
      if owner.isDefDef then

         owner.tree match
            case d @ DefDef(name, parameters, _, _) =>
               parameters
                  .collectFirst {
                     case TypeParamClause(_) =>
                        report.errorAndAbort(
                          "Cannot generate C bind from generic method"
                        )
                     case t @ TermParamClause(valDefs) =>
                        val params =
                           valDefs.map(t => Ref(t.symbol).asExpr.widen)
                        (name, params)
                  }
                  .getOrElse(
                    (name, Nil)
                  )
      else
         report.errorAndAbort(
           s"didn't get defdef ${owner.tree.show(using Printer.TreeAnsiCode)}"
         )

   val memoryAddress = '{
      val symbolLookup = ${ Expr.summonOrError[SymbolLookup] }
      val fnName = ${ Expr(name) }
      symbolLookup.lookup(fnName)
   }

   MethodHandleMacros
      .binding[R](memoryAddress, params)
      .tap { exp =>
         if debug then report.info(exp.show)
      }
      .asExprOf[R]

end bindImpl

def scope[A](fn: ResourceScope ?=> Allocatee[A])(using NotGiven[A <:< Ptr[?]]) =
   given resourceScope: ResourceScope = ResourceScope.newConfinedScope
   given SegmentAllocator = SegmentAllocator.arenaAllocator(resourceScope)
   try {
      fn
   } finally {
      resourceScope.close
   }

def globalScope[A](fn: Scopee[Allocatee[A]]) =
   given resourceScope: ResourceScope = ResourceScope.globalScope
   given SegmentAllocator = SegmentAllocator.arenaAllocator(resourceScope)
   fn

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

extension [A: ClassTag](
    a: Array[A]
)
   def serialize: Allocatee[Scopee[Informee[A, Exportee[Array[A], Ptr[A]]]]] =
      val addr = exportValue(a)
      Ptr[A](addr, 0)

extension [A, S <: Iterable[A]](s: S)
   def serialize: Informee[A, Serializee[A, Allocatee[Ptr[A]]]] =
      val addr = segAlloc.allocateArray(layoutOf[A], s.size).address
      s.zipWithIndex.foreach((a, i) =>
         serializerOf[A].into(a, addr, i * layoutOf[A].byteSize)
      )
      Ptr[A](addr, 0)

extension [A](a: A)
   def serialize: Allocatee[Scopee[Informee[A, Exportee[A, Ptr[A]]]]] =
      val addr = exportValue(a)
      Ptr[A](addr, 0)

def sizeOf[A]: Informee[A, SizeT] = SizeT.fromLongOrFail(layoutOf[A].byteSize)

extension (s: String)
   def serialize: Allocatee[Ptr[Byte]] =
      Ptr[Byte](CLinker.toCString(s, segAlloc).address, 0)

def allocate[A](long: Long): Informee[A, Allocatee[Ptr[A]]] =
   Ptr[A](segAlloc.allocate(long * layoutOf[A].byteSize).address, 0)

export components.HelperTypes.*
export components.Variadic.variadicBind
export components.platform.*

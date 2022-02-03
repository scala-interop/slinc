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
   Encodee,
   Exportee,
   Scopee,
   SymbolLookup,
   segAlloc,
   encoderOf,
   layoutOf,
   exportValue,
   summonOrError,
   Platform,
   FromNative,
   widen,
   Cache,
   findClass,
   findMethod,
   VariadicCache
}

/** Macro for generating a binding to C. Uses the method name and parameter
  * types of the method it's bound to for binding generation
  *
  * @tparam R
  *   The return type of the binding
  * @return
  *   Binding code for the C function
  * @example
  * ```scala
  * def abs(i: Int) = bind[Int]
  * ```
  * @note
  *   binding does not work on generic methods
  */
inline def bind[R](using
    @implicitNotFound(
      "You must provide a return type for bind"
    ) n: NotGiven[R =:= Nothing]
): R = ${
   bindImpl[R]('false)
}

/** Macro for generating a binding to C. Uses the method name and parmeter types
  * of the method it's assigned to for binding generation
  * @param debug
  *   Tells the compiler whether to print info about the generated code.
  * @tparam R
  *   The return type of the binding
  * @return
  *   Binding code for the C function
  * @example
  * ```scala
  * def abs(i: Int) = bind[Int]
  * ```
  * @note
  *   binding does not work on generic methods
  */
inline def bind[R](debug: Boolean)(using
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

   val obj = Symbol.spliceOwner.owner.owner

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

/** Provides an allocation scope.
  * @tparam A
  *   The return type to exit the scope
  * @param fn
  *   The block of code that needs a scope for allocation. Once the block ends,
  *   the memory allocated by this scope is freed.
  * @note
  *   This function has a guard against returning
  *   [[io.gitlab.mhammons.slinc.Ptr]]s. Since native memory allocated by this
  *   scope will be freed at its end, it's almost always an error to return
  *   pointers from this scope. If you need to have persistent native memory,
  *   use [[io.gitlab.mhammons.slinc.globalAllocator]] instead.
  * @example
  * ```scala
  * val length = scope {
  *   val strP: Ptr[Byte] = "hello world".serialize
  *   strlen(strp)
  * }
  * ```
  * @since v0.1.0
  */
def scope[A](fn: Scopee[Allocatee[A]])(using NotGiven[A <:< Ptr[?]]) =
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

def lazyScope[A](fn: Scopee[Allocatee[A]]) =
   given resourceScope: ResourceScope = ResourceScope.newImplicitScope
   given SegmentAllocator = SegmentAllocator.arenaAllocator(resourceScope)
   fn

extension [A: ClassTag](
    a: Array[A]
)
   def encode: Allocatee[Scopee[Informee[A, Exportee[Array[A], Ptr[A]]]]] =
      val addr = exportValue(a)
      Ptr[A](addr, 0)

extension [A, S <: Iterable[A]](s: S)
   def encode: Informee[A, Encodee[A, Allocatee[Ptr[A]]]] =
      val addr = segAlloc.allocateArray(layoutOf[A], s.size).address
      s.zipWithIndex.foreach((a, i) =>
         encoderOf[A].into(a, addr, i * layoutOf[A].byteSize)
      )
      Ptr[A](addr, 0)

extension [A](a: A)
   def encode: Allocatee[Scopee[Informee[A, Exportee[A, Ptr[A]]]]] =
      val addr = exportValue(a)
      Ptr[A](addr, 0)

/** Returns the native size of the input type
  * @tparam A
  *   The type to get a native size of
  * @return
  *   The native size of type A
  * @since v0.1.0
  */
def sizeOf[A]: Informee[A, SizeT] = SizeT.fromLongOrFail(layoutOf[A].byteSize)

extension (s: String)
   def encode: Allocatee[Ptr[Byte]] =
      Ptr[Byte](CLinker.toCString(s, segAlloc).address, 0)

/** Allocates a blank block of native heap
  * @tparam A
  *   The native type you want to allocate chunks for
  * @param num
  *   The number of chunks to allocate
  * @return
  *   A [[io.gitlab.mhammons.slinc.Ptr]] of A that points to num chunks of
  *   native heap
  * @note
  *   Needs a scope to do allocation
  * @example
  * ```scala
  * val intSegment = globalScope {
  *   allocate[Int](1)
  * }
  * ```
  */
def allocate[A](num: Long): Informee[A, Allocatee[Ptr[A]]] =
   Ptr[A](segAlloc.allocate(num * layoutOf[A].byteSize).address, 0)

export components.HelperTypes.*
export components.Variadic.variadicBind
export components.platform.*

inline def accessNative[R] = ${
   bind2Impl[R]
}

def bind2Impl[R](using Quotes, Type[R]): Expr[R] =
   import quotes.reflect.*

   val foundClass = findClass(Symbol.spliceOwner)
   val foundMethod = findMethod(Symbol.spliceOwner)

   val foundIndex = foundClass.declaredMethods
      .indexOf(foundMethod)
      .pipe {
         case -1 =>
            report.errorAndAbort(
              s"${foundMethod.fullName} is not considered a native method. This is a serious error"
            )
         case i => i
      }

   val ((inputRefs, inputTypes), returnType) = foundMethod.tree match
      case DefDef(_, params, retType, _) =>
         params
            .flatMap(_.params)
            .collect { case v @ ValDef(r, t, _) =>
               Ref(v.symbol) -> t
            }
            .unzip -> retType

   val fnSymbol = Symbol.requiredClass(s"scala.Function${inputRefs.size}")
   val fnTypeComplete = Applied(
     TypeIdent(fnSymbol),
     inputTypes :+ returnType
   ).tpe.asType
   (TypeIdent(foundClass).tpe.asType, fnTypeComplete) match
      case ('[o], '[f]) =>
         val lib = Expr.summonOrError[CLibrary[o]]
         val lambda = '{
            $lib.cache
               .getCached[f](${ Expr(foundIndex) })
         }.asExprOf[f]

         Apply(
           Select(
             lambda.asTerm,
             fnSymbol.declaredMethod("apply").head
           ),
           inputRefs
         ).asExprOf[R]

transparent inline def accessNativeVariadic[R](inline args: Any*)(using
    @implicitNotFound(
      "You must provide a return type for nativeFromVariadic"
    ) n: NotGiven[R =:= Nothing]
) = ${
   accessNativeVariadicImpl[R]('args)
}

private def accessNativeVariadicImpl[R](
    args: Expr[Seq[Any]]
)(using Quotes, Type[R]) =
   import quotes.reflect.*
   val foundClass = findClass(Symbol.spliceOwner)
   val foundMethod = findMethod(Symbol.spliceOwner)
   val foundIndex = foundClass.declaredMethods.indexOf(foundMethod).match {
      case -1 =>
         report.errorAndAbort(
           s"${foundMethod.fullName} is not considered a variadic method. This is a serious error, please report it."
         )
      case i => i
   }

   val inputRefs = args match
      case Varargs(exprs) =>
         exprs.map(_.asTerm).toList

   TypeIdent(foundClass).tpe.asType match
      case '[o] =>
         val lib = Expr
            .summon[CLibrary[o]]
            .getOrElse(
              report.errorAndAbort(
                s"No native bindings to access, since ${Type.show[o]} is not a library."
              )
            )
         val variadicCache = '{
            $lib.cache.getCached[VariadicCache[R]](${ Expr(foundIndex) })
         }

         '{
            $variadicCache.apply(${ Varargs(inputRefs.map(_.asExpr)) }*)
         }

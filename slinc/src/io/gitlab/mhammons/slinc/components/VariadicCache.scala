package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import jdk.incubator.foreign.{SymbolLookup, MemoryAddress}

class VariadicCache[R](name: String, symbolLookup: SymbolLookup):
   val cache: ThreadLocal[LRU] = ThreadLocal.withInitial(() => new LRU(10))
   val address = symbolLookup
      .lookup(name)
      .orElseGet(() => throw new Exception(s"Could not find method ${name}"))

   transparent inline def apply(inline args: Any*) = ${
      VariadicCache.applyImpl[R]('address, '{ cache.get }, 'args)
   }

object VariadicCache:
   def applyImpl[R](
       address: Expr[MemoryAddress],
       cache: Expr[LRU],
       argsExpr: Expr[Seq[Any]]
   )(using
       Quotes,
       Type[R]
   ): Expr[Any] =
      import quotes.reflect.*

      val params = argsExpr match
         case Varargs(exprs) =>
            exprs.map { case '{ $e: a } =>
               TypeRepr.of[a].widen.asType.match { case '[b] =>
                  e.asExprOf[b] -> TypeTree.of[b]
               }
            }.toList
      val arity = params.size
      val symbol = TypeRepr
         .of[VariadicCalls.type]
         .typeSymbol
         .declaredTypes
         .filter(s =>
            s.name.endsWith(arity.toString) && s.name.startsWith("Cached")
         )
         .head
      val tt = Applied(TypeIdent(symbol), params.map(_._2) :+ TypeTree.of[R])
      Apply(
        TypeApply(
          Select(
            New(tt),
            symbol.primaryConstructor
          ),
          params.map(_._2) :+ TypeTree.of[R]
        ),
        address.asTerm +: cache.asTerm +: params.map(_._1.asTerm)
      ).asExpr

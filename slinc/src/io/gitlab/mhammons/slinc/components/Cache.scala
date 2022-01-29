package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.SymbolLookup

class Cache(storage: Array[Any]):
   def getCached[A](key: Int): A = storage(key).asInstanceOf[A]

object Cache:
   transparent inline def apply[A](symbolLookup: SymbolLookup): Cache = ${
      cacheImpl[A]('symbolLookup)
   }

   def getCachedSymbols(using q: Quotes)(
       p: q.reflect.Symbol
   ): List[q.reflect.Symbol] =
      import quotes.reflect.*
      val blacklist = Set("writeReplace")

      def isNotNative(s: Symbol) = s.annotations.exists {
         case Apply(Select(New(tpt), _), _)
             if tpt.tpe =:= TypeRepr.of[NonNative] =>
            true
         case _ => false
      }
      p.declaredMethods.filterNot(s =>
         blacklist.contains(s.name) || isNotNative(s)
      )

   private def cacheImpl[A](
       symbExpr: Expr[SymbolLookup]
   )(using Quotes, Type[A]) =
      import quotes.reflect.*

      def isNotNative(s: Symbol) =
         s.annotations.exists {
            case Apply(Select(New(tpt), _), _)
                if tpt.tpe =:= TypeRepr.of[NonNative] =>
               true
            case _ =>
               false
         }

      val blacklist = Set("writeReplace")
      TypeRepr
         .of[A]
         .classSymbol
         .tap(_.map(_.name))
         .map(cs =>
            getCachedSymbols(cs)
               .map(s => MethodHandleMacros.wrappedMHFromDefDef(s, symbExpr))
         )
         .map { s =>
            '{
               new Cache(Array(${ Varargs(s) }*))
            }
         }
         .getOrElse(
           report.errorAndAbort("messed up ")
         )

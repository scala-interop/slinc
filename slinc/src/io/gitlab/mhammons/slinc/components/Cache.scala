package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import scala.util.Using
import jdk.incubator.foreign.SymbolLookup
import scala.runtime.stdLibPatches.language.deprecated.symbolLiterals

trait Table[A](arr: Array[A]):
   def resolve(key: String): Int
   def apply(key: String): A = arr(resolve(key))
   def update(key: String, value: A): Unit = arr(resolve(key)) = value

class EmptyTable[A] extends Table[A](null):
   def resolve(key: String) = ???

class Cache(storage: Array[Any]):
   def getCached[A](key: Int): A = storage(key).asInstanceOf[A]

object Cache:
   transparent inline def apply[A](symbolLookup: SymbolLookup): Cache = ${
      cacheImpl[A]('symbolLookup)
   }

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

      TypeRepr
         .of[A]
         .classSymbol
         .tap(_.map(_.name))
         .map(cs =>
            cs.declaredMethods
               .filterNot(isNotNative)
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

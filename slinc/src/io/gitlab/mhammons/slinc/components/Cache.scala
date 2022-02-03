package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.SymbolLookup
import cats.data.Validated

class Cache(storage: Array[Any]):
   def getCached[A](key: Int): A = storage(key).asInstanceOf[A]

object Cache:
   transparent inline def apply[A](symbolLookup: SymbolLookup): Cache = ${
      cacheImpl[A]('symbolLookup)
   }

   def isCachedSymbol(using q: Quotes)(
       s: q.reflect.Symbol
   ): Boolean =
      import quotes.reflect.*
      val blacklist = Set("writeReplace")

      def isNotNative(s: Symbol) = s.annotations.exists {
         case Apply(Select(New(tpt), _), _)
             if tpt.tpe =:= TypeRepr.of[NonNative] =>
            true
         case _ => false
      }

      !(blacklist.contains(s.name) || isNotNative(s))

   def isVariadic(using q: Quotes)(
       s: q.reflect.Symbol
   ): Boolean =
      import quotes.reflect.*

      s.tree match
         case DefDef(_, _, ret, _) =>
            ret.tpe.asType match
               case '[VariadicCalls.VariadicCall] => true
               case _                             => false

   private def cacheImpl[A](
       symbExpr: Expr[SymbolLookup]
   )(using Quotes, Type[A]) =
      import quotes.reflect.*

      TypeRepr
         .of[A]
         .classSymbol
         .tap(_.map(_.name))
         .map(cs =>
            cs.declaredMethods
               .map(s =>
                  if isVariadic(s) then
                     Validated.valid(List('{
                        VariadicCache(${ Expr(s.name) }, $symbExpr)
                     }))
                  else if isCachedSymbol(s) then
                     MethodHandleMacros
                        .wrappedMHFromDefDef(s, symbExpr)
                        .bimap(
                          es =>
                             List(
                               s"Errors while generating binding for ${s.fullName}:\n\t- ${es.mkString("\n\t- ")}"
                             ),
                          List(_)
                        )
                  else Validated.valid(List('{ null }))
               )
               .reduce((a, b) => a.andThen(subA => b.map(subA ++ _)))
         )
         .map { s =>
            '{
               new Cache(Array(${
                  Varargs(
                    s.fold(
                      es => report.errorAndAbort(es.mkString("\n")),
                      identity
                    )
                  )
               }*))
            }
         }
         .getOrElse(
           report.errorAndAbort(
             s"Class not found for type ${Type.show[A]}... Are you sure you tried to derive CLibrary for a class or object?"
           )
         )

package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.MemoryLayout
import io.gitlab.mhammons.slinc.components.MemLayout
import io.gitlab.mhammons.slinc.components.StructLayout
import components.StructInfo
import scala.collection.concurrent.TrieMap
import scala.deriving.Mirror.ProductOf
import io.gitlab.mhammons.slinc.components.StructElement

object LayoutMacros:
   inline def layoutName[A] = ${ layoutNameImpl[A] }
   def layoutNameImpl[A: Type](using Quotes) =
      import quotes.reflect.report
      Type.of[A] match
         case '[Struct] =>
            StructMacros
               .getStructInfo[A]
               .members
               .map {
                  case StructElement(name, '[a]) =>
                     s"$name:${Type.show[a]}"
               }
               .mkString(",")
               .pipe(Expr.apply)
         case '[r] =>
            report.errorAndAbort(
              s"received type ${Type.show[r]}. I cannot process it. Sorry..."
            )

   inline def layoutIndex[A] = ${layoutIndexImpl[A]}

   def layoutIndexImpl[A: Type](using Quotes) =
      val name = layoutNameImpl.valueOrAbort
      Expr(components.UniversalNativeCache.getLayoutIndex(name))

   inline def deriveLayout2[A]: StructLayout = ${
      deriveLayoutImpl2[A]
   }

   private def deriveLayoutImpl2[A: Type](using q: Quotes): Expr[StructLayout] =
      import TransformMacros.type2MemLayout
      import quotes.reflect.report
      Type.of[A] match
         case '[Struct] =>
            val structInfo = StructMacros
               .getStructInfo[A]

            val expr = structInfo.members
               .map {
                  case StructElement(name, '[a]) =>
                     '{ ${ type2MemLayout[a] }.withName(${ Expr(name) }) }
               }
               .pipe(Expr.ofSeq)

            '{
               StructLayout($expr)
            }

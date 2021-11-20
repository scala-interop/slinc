package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import jdk.incubator.foreign.MemoryLayout
import io.gitlab.mhammons.slinc.components.MemLayout
import io.gitlab.mhammons.slinc.components.StructLayout
import components.{PrimitiveInfo, StructInfo, StructStub}
import scala.collection.concurrent.TrieMap

object LayoutMacros:
   private val layoutCache = TrieMap.empty[String, MemLayout]
   inline def layoutName[A] = ${ layoutNameImpl[A] }
   def layoutNameImpl[A: Type](using Quotes) =
      import quotes.reflect.report
      Type.of[A] match
         case '[Struct] =>
            StructMacros
               .getStructInfo[A]
               .members
               .map {
                  case PrimitiveInfo(name, '[a]) =>
                     s"$name:${Type.show[a]}"
                  case StructStub(name, '[a]) =>
                     s"$name:${Type.show[a]}"
               }
               .mkString(",")
               .pipe(Expr.apply)
         case '[r] =>
            report.errorAndAbort(
              s"received type ${Type.show[r]}. I cannot process it. Sorry..."
            )

   inline def deriveLayout2[A]: StructLayout = ${
      deriveLayoutImpl2[A]
   }

   private def deriveLayoutImpl2[A: Type](using q: Quotes): Expr[StructLayout] =
      import TransformMacros.type2MemLayout
      import quotes.reflect.report
      Type.of[A] match
         case '[Struct] =>
            // todo: rename refinementDataExtraction2 to StructLikeDataExtraction
            val structInfo = StructMacros
               .getStructInfo[A]

            val expr = structInfo.members
               .map {
                  case PrimitiveInfo(name, '[a]) =>
                     '{ ${ type2MemLayout[a] }.withName(${ Expr(name) }) }
                  case StructStub(name, '[a]) =>
                     '{ ${ deriveLayoutImpl2[a] }.withName(${ Expr(name) }) }

               }
               .pipe(Expr.ofSeq)

            '{
               StructLayout($expr)
            }

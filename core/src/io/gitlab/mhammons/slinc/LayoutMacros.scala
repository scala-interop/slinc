package io.gitlab.mhammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import cats.catsInstancesForId
import cats.implicits.*
import jdk.incubator.foreign.MemoryLayout
import io.gitlab.mhammons.slinc.components.MemLayout
import io.gitlab.mhammons.slinc.components.StructLayout

object LayoutMacros:
   inline def layoutName[A] = ${ layoutNameImpl[A] }
   def layoutNameImpl[A: Type](using Quotes) =
      import quotes.reflect.report
      Type.of[A] match
         case '[Struct] =>
            StructMacros
               .refinementDataExtraction[A]
               .reverse
               .map { case (name, '[a]) =>
                  s"$name:${Type.show[a]}"
               }
               .mkString(",")
               .pipe(Expr.apply)
         case '[r] =>
            report.errorAndAbort(
              s"received type ${Type.show[r]}. I cannot process it. Sorry..."
            )

   inline def deriveLayout[A]: MemoryLayout = ${
      deriveLayoutImpl[A]
   }
   def deriveLayoutImpl[A: Type](using
       q: Quotes
   ): Expr[MemoryLayout] =
      import TransformMacros.type2MemoryLayout
      Type.of[A] match
         case '[Struct] =>
            val fieldLayouts = StructMacros
               .refinementDataExtraction[A]
               .reverse
               .map { case (name, '[a]) =>
                  '{
                     ${ type2MemoryLayout[a] }.withName(${ Expr(name) })
                  }
               }
               .pipe(Expr.ofSeq)

            '{
               MemoryLayout.structLayout($fieldLayouts*)
            }

   inline def deriveLayout2[A]: MemLayout = ${
      deriveLayoutImpl2[A]
   }

   private def deriveLayoutImpl2[A: Type](using q: Quotes): Expr[MemLayout] =
      import TransformMacros.type2MemLayout
      import quotes.reflect.report
      Type.of[A] match
         case '[Struct] | '[components.Struct] =>
            // todo: rename refinementDataExtraction2 to StructLikeDataExtraction
            val fieldLayouts =
               StructMacros
                  .refinementDataExtraction2[A]()
                  .reverse
                  .map { case (names, '[a]) =>
                     names -> type2MemLayout[a]
                  }
                  .toList

            report.info(fieldLayouts.map(_.toString).mkString)

            buildStructLayouts(fieldLayouts)

   private def buildStructLayouts(
       namedLayouts: Seq[(Seq[String], Expr[MemLayout])]
   )(using Quotes): Expr[StructLayout] =
      val levelKeys = namedLayouts.map(_._1.head).distinct
      val levelExtracted = namedLayouts
         .groupMap(_._1.head)((names, layout) => (names.tail, layout))

      val layouts: Expr[Seq[(String, MemLayout)]] = levelKeys
         .map(name =>
            levelExtracted(name) match
               case Seq((Nil, layout)) =>
                  '{ (${ Expr(name) }, $layout) }
               case members =>
                  '{ (${ Expr(name) }, ${ buildStructLayouts(members) }) }
         )
         .pipe(Varargs.apply)

      '{
         StructLayout($layouts*)
      }

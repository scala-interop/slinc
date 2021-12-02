package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*

import jdk.incubator.foreign.{
   MemoryLayout,
   CLinker,
   MemoryAddress,
   MemorySegment
}, CLinker.{C_INT, C_FLOAT, C_DOUBLE, C_LONG, C_POINTER}

import io.gitlab.mhammons.slinc.Struct
import io.gitlab.mhammons.slinc.StructMacros
import io.gitlab.mhammons.slinc.LayoutMacros

trait LayoutOf[A]:
   val layout: MemoryLayout
   val carrierType: Class[?]

object LayoutOf:
   given LayoutOf[Int] with
      val layout = C_INT
      val carrierType = classOf[Int]

   given LayoutOf[Float] with
      val layout = C_FLOAT
      val carrierType = classOf[Float]

   given LayoutOf[Double] with
      val layout = C_DOUBLE
      val carrierType = classOf[Double]

   given LayoutOf[Long] with
      val layout = C_LONG
      val carrierType = classOf[Long]

   given LayoutOf[String] with
      val layout = C_POINTER
      val carrierType = classOf[MemoryAddress]

   val pointerLayout = new LayoutOf[Ptr[Any]]:
      val layout = C_POINTER
      val carrierType = classOf[MemoryAddress]
   given [A]: LayoutOf[Ptr[A]] = pointerLayout.asInstanceOf[LayoutOf[Ptr[A]]]

   given [T](using LayoutOf[T]): LayoutOf[Member[T]] =
      summon[LayoutOf[T]].asInstanceOf[LayoutOf[Member[T]]]

   inline given [T]: LayoutOf[T] = ${
      deriveLayoutImpl[T]
   }

   private def deriveLayoutImpl[T: Type](using q: Quotes) =
      import quotes.reflect.report

      Type.of[T] match
         case '[Product] =>
            ???

         case '[Struct] =>
            val (index, subLayouts) = StructMacros
               .getStructInfo[T]
               .members
               .map { case StructElement(name, '[a]) =>
                  s"$name:${Type.of[a]}" ->
                     Expr
                        .summon[LayoutOf[a]]
                        .getOrElse(missingLayout[T])
                        .pipe(exp =>
                           '{ ${ exp }.layout.withName(${ Expr(name) }) }
                        )
               }
               .pipe(_.unzip)
               .pipe((namePieces, layoutPieces) =>
                  namePieces
                     .mkString(",")
                     .pipe(UniversalNativeCache.getLayoutIndex) -> Expr.ofSeq(
                    layoutPieces
                  )
               )
            '{
               UniversalNativeCache.getLayout[T](
                 ${ Expr(index) },
                 new LayoutOf[T]:
                    val layout = MemoryLayout.structLayout($subLayouts*)
                    val carrierType = classOf[MemorySegment]
               )
            }

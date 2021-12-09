package io.gitlab.mhammons.slinc.components

import scala.quoted.*
import scala.util.chaining.*

import io.gitlab.mhammons.slinc.Ptr

import jdk.incubator.foreign.{
   MemoryLayout,
   CLinker,
   MemoryAddress,
   MemorySegment
}, CLinker.{C_INT, C_FLOAT, C_DOUBLE, C_LONG, C_POINTER}

trait LayoutOf[A]:
   val layout: MemoryLayout
   val carrierType: Class[?]

object LayoutOf:
   def fromTypeInfo(typeInfo: TypeInfo)(using Quotes): Expr[MemoryLayout] =
      typeInfo match
         case PrimitiveInfo(name, '[a]) =>
            '{
               ${ Expr.summonOrError[LayoutOf[a]] }.layout.withName(${
                  Expr(name)
               })
            }
         case PtrInfo(name, _, _) => '{ C_POINTER.withName(${ Expr(name) }) }
         case ProductInfo(name, members, _) =>
            '{
               MemoryLayout
                  .structLayout(${
                     members.map(fromTypeInfo).pipe(Expr.ofSeq)
                  }*)
                  .withName(${ Expr(name) })
            }

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

   object PtrLayout extends LayoutOf[Ptr[?]]:
      val layout = C_POINTER
      val carrierType = classOf[MemoryAddress]

   given [A]: LayoutOf[Ptr[A]] = PtrLayout.asInstanceOf[LayoutOf[Ptr[A]]]

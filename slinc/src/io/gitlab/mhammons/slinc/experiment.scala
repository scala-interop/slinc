package io.gitlab.mhammons.slinc

import scala.deriving.Mirror.ProductOf
import scala.language.dynamics

type GetMatch[Tup <: Tuple, K] = Tup match
   case EmptyTuple                     => Nothing
   case (K *: t *: EmptyTuple) *: ?    => t
   case (? *: ? *: EmptyTuple) *: rest => GetMatch[rest, K]

type NoMatch[Tup <: Tuple, K] = Tup match
   case (K *: ? *: EmptyTuple) *: ?    => false
   case (? *: ? *: EmptyTuple) *: rest => NoMatch[rest, K]
   case EmptyTuple                     => true

class HMap[A <: Tuple](map: Map[Any, Any]):
   def get[K <: Singleton](k: K)(using
       NoMatch[A, K] =:= false
   ): GetMatch[A, K] = map(k).asInstanceOf[GetMatch[A, K]]

class PsuedoCaseClass[T <: Tuple](hmap: HMap[T]) extends Dynamic:
   def selectDynamic[K <: Singleton](k: K)(using ev: NoMatch[T, K] =:= false) =
      hmap.get(k)(using ev)

class Pt[A](val a: A)(using ProductOf[A])

case class Wrap[A](`unary_!`: A):
   def `unary_!_=`(a: A) = println("hello world")

class Pt1(val map: Map[String, Any]) extends Selectable:
   def selectDynamic(key: String) = map(key)

extension [A <: Product](a: Pt[A])(using ProductOf[A])
   transparent inline def dual =
      val productOf = summon[ProductOf[A]]
      type DualType = Tuple.Zip[
        productOf.MirroredElemLabels,
        Tuple.Map[productOf.MirroredElemTypes, Pt]
      ]
      PsuedoCaseClass[DualType](
        HMap[DualType](a.a.productElementNames.zip(a.a.productIterator).toMap)
      )

package fr.hammons.sffi

import scala.compiletime.ops.int.+
import scala.deriving.Mirror

trait PtrShape[A, B <: Tuple]:
  import scala.compiletime.constValue

object PtrShape:
  given PtrShape[Int, EmptyTuple] with {}

  given PtrShape[Byte, EmptyTuple] with {}

  given [P <: Product, T <: Tuple](using m: Mirror.ProductOf[P])(using
      T =:= Tuple.Zip[m.MirroredElemLabels, m.MirroredElemTypes]
  ): PtrShape[P, T] with {}

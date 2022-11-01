package fr.hammons.slinc.container

import scala.compiletime.ops.int.+
import scala.compiletime.constValue
import scala.compiletime.summonAll
import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.quoted.*

class ContextProof[C <: Capabilities, A](val tup: ContextProof.ToTuple[C, A])

object ContextProof:
  inline def apply[C <: Capabilities, A](): ContextProof[C, A] =
    new ContextProof(summonAll[ToTuple[C, A]])

  type ToTuple[A <: Capabilities, T] <: Tuple = A match
    case head *::: tail => head[T] *: ToTuple[tail, T]
    case End            => EmptyTuple

  inline given reducedProof[A, Cap <: Capabilities, C[_], N <: Int](using
      c: ContextProof[Cap, A],
      l: LocationInCap[C, Cap, N]
  ): ContextProof[C *::: End, A] = new ContextProof(
    (c.tup.productElement(constValue[N]).asInstanceOf[C[A]]) *: EmptyTuple
  )

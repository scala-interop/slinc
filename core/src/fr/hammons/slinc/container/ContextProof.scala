package fr.hammons.slinc.container

import scala.compiletime.constValue
import scala.compiletime.summonAll
import scala.annotation.nowarn

class ContextProof[C <: Capabilities, A](val tup: ContextProof.ToTuple[C, A])

type SingleProof[C[_], A]
object ContextProof:
  inline def apply[C <: Capabilities, A](): ContextProof[C, A] =
    new ContextProof(summonAll[ToTuple[C, A]])

  type ToTuple[A <: Capabilities, T] <: Tuple = A match
    case head *::: tail => head[T] *: ToTuple[tail, T]
    case End            => EmptyTuple

  // todo: replace with `erased` for l once that's not experimental
  @nowarn("msg=unused implicit parameter")
  inline given reducedProof[A, Cap <: Capabilities, C[_], N <: Int](using
      c: ContextProof[Cap, A],
      l: LocationInCap[C, Cap, N]
  ): ContextProof[C *::: End, A] = new ContextProof(
    (c.tup.productElement(constValue[N]).asInstanceOf[C[A]]) *: EmptyTuple
  )

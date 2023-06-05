package fr.hammons.slinc.container

class ContextSet[T <: Tuple](val t: T)

object ContextSet:
  given reducedSet[A, C <: Capabilities, T <: Tuple](using
      c: ContextSet[ContextProof[C, A] *: T]
  ): ContextSet[ContextProof[C, A] *: EmptyTuple] = ContextSet(
    c.t.head *: EmptyTuple
  )
  given setToReduce[A, B, C <: Capabilities, D <: Capabilities, T <: Tuple](
      using c: ContextSet[ContextProof[D, B] *: ContextProof[C, A] *: T]
  ): ContextSet[ContextProof[C, A] *: T] = ContextSet(c.t.tail)

  given proofFromSet[A, C <: Capabilities](using
      c: ContextSet[ContextProof[C, A] *: EmptyTuple]
  ): ContextProof[C, A] = c.t.head

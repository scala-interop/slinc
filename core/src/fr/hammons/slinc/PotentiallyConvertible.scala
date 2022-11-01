package fr.hammons.slinc

import container.*

trait PotentiallyConvertible[A, B]:
  def to(a: A): Option[B]

object PotentiallyConvertible:
  extension [A](a: A)
    def maybeAs[B](using pc: PotentiallyConvertible[A, B]) = pc.to(a)
  given PotentiallyConvertible[Long, Int] with
    def to(a: Long): Option[Int] =
      if a <= Int.MaxValue && a >= Int.MinValue then Some(a.toInt)
      else None

  given PotentiallyConvertible[Long, Long] with
    def to(a: Long): Option[Long] = Some(a)

  transparent inline given [A, B, Cap <: Capabilities](using
      c: ContextProof[PotentiallyConvertible[*, B] *::: End, A]
  ): PotentiallyConvertible[A, B] = c.tup.head

  transparent inline given [A, B, Cap <: Capabilities](using
      c: ContextProof[PotentiallyConvertible[B, *] *::: End, A]
  ): PotentiallyConvertible[B, A] = c.tup.head
  given PotentiallyConvertible[Int, Int] with
    def to(a: Int): Option[Int] = Some(a)

  given PotentiallyConvertible[Int, Long] with
    def to(a: Int): Option[Long] =
      Some(a.toLong)

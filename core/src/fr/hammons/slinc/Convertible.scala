package fr.hammons.slinc

import container.*

trait Convertible[A, B]:
  def to(a: A): B

object Convertible:
  extension [A](a: A) def as[B](using conv: Convertible[A, B]): B = conv.to(a)

  given Convertible[Int, Int] with
    def to(a: Int): Int = a

  given Convertible[Int, Long] with
    def to(a: Int): Long = a.toLong

  given Convertible[Long, Long] with
    def to(a: Long): Long = a

  inline given [A, B](using
      c: ContextProof[Convertible[*, B] *::: End, A]
  ): Convertible[A, B] = c.tup.head

  inline given [A, B](using
      c: ContextProof[Convertible[B, *] *::: End, A]
  ): Convertible[B, A] = c.tup.head

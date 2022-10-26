package fr.hammons.slinc

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

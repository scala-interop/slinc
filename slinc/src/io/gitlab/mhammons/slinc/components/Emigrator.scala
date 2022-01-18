package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{CLinker, SegmentAllocator}

type Emigratee[A, B] = Emigrator[A] ?=> B

def emigrator[A]: Emigratee[A, Emigrator[A]] = summon[Emigrator[A]]
def emigrate[A](a: A): Allocatee[Emigratee[A, Any]] = emigrator[A](a)

trait Emigrator[A]:
   def apply(a: A): Allocatee[Any]

object Emigrator:
   given Emigrator[Int] with
      def apply(a: Int) = a

   given Emigrator[Long] with
      def apply(a: Long) = a
   given Emigrator[String] with
      def apply(a: String) = CLinker.toCString(a, segAlloc).address

   given Emigrator[Double] with
      def apply(a: Double) = a

   given Emigrator[Float] with
      def apply(a: Float) = a

   given Emigrator[Byte] with
      def apply(a: Byte) = a

   given Emigrator[Short] with
      def apply(a: Short) = a

   given Emigrator[Boolean] with
      def apply(a: Boolean) = if a then 1.toByte else 0.toByte

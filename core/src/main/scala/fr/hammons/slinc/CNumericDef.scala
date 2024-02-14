package fr.hammons.slinc

import util.NotGiven

trait CNumericDefMin[A <: CVal[A], B] extends CValDef[A, B] {
  type MakeOutput = A
  type RetrieveOutput = Option[B]
}

object CNumericDefMin:
  given inferredLong[A <: CVal[A], B](using
      CNumericDefMin[A, B],
      NotGiven[CValDef[A, Long]]
  ): CValDef[A, Long] with {
    type MakeOutput = Option[A]
    type RetrieveOutput = B
    def make(b: Long): Option[A] = ???
    def retrieve(a: A): RetrieveOutput = ???
  }

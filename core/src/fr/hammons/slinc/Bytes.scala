package fr.hammons.slinc

import scala.quoted.{ToExpr, Quotes}
import fr.hammons.slinc.types.SizeT

opaque type Bytes = Long

object Bytes:
  def apply(l: Long): Bytes = l

  extension (a: Bytes)
    inline def +(b: Bytes): Bytes = a + b
    inline def *(i: Int): Bytes = a * i
    inline def *(b: Bytes): Bytes = a * b
    inline def %(b: Bytes): Bytes = a % b
    inline def -(b: Bytes): Bytes = a - b
    inline def toLong: Long = a
    inline def toBits: Long = a * 8
    def toSizeT = SizeT
      .maybe(a)
      .getOrElse(
        throw new Exception(
          s"The bytes described was too big for the current platform $a"
        )
      )

  given Numeric[Bytes] = Numeric.LongIsIntegral
  given ToExpr[Bytes] with
    def apply(t: Bytes)(using Quotes) = ToExpr.LongToExpr[Long].apply(t)

package fr.hammons.slinc.internal

import fr.hammons.slinc.Platform

opaque type CIntegral <: CVal = CVal

object CIntegral:
  private[slinc] def apply[A <: AnyVal, B, P <: Platform](
      l: A
  )(using P, TypeRelation[P, B, A]): CIntegral = CVal(l)

  extension (cintegral: CIntegral)
    private[slinc] def asLong = cintegral.as[Long]

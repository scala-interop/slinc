package fr.hammons.slinc

import fr.hammons.slinc.internal.CIntegral
import fr.hammons.slinc.internal.LightOption
import fr.hammons.slinc.internal.TypeRelation

opaque type CInt <: CIntegral = CIntegral

object CInt:
  // given (using Runtime): Numeric[CInt] = CIntegral.numericDerivation[CInt]
  given TypeRelation[Platform.All, CInt] with {
    type Real = Int
  }

  def apply(i: Int)(using r: Runtime): CInt =
    r.platform match
      case given Platform.All =>
        CIntegral(i)
  def apply(l: Long)(using r: Runtime): LightOption[CInt] =
    r.platform match
      case given Platform.All =>
        if l <= Int.MaxValue && l >= Int.MinValue then
          LightOption(CIntegral(l.toInt))
        else LightOption.None

  extension (cint: CInt)
    def toLong = cint.asLong.getOrThrow()
    def toInt = cint.asLong.getOrThrow().toInt
    def toShort = cint.asLong.flatMap(l =>
      if l <= Short.MaxValue && l >= Short.MinValue then LightOption(l.toShort)
      else LightOption.None
    )
//   def getAs[B](using cvd: CValDef[CInt, B]): cvd.GetOutput = ???
//   def set[B](using cvd: CValDef[CInt, B])(a: B): cvd.AssignOutput = ???

// object CInt:
//   given CValDef[CInt, Int] with
//     type GetOutput = Int
//     type SetOutput = Unit

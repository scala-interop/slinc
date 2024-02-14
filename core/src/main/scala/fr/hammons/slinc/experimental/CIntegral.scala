package fr.hammons.slinc.experimental

import fr.hammons.slinc.Platform
import fr.hammons.slinc.Runtime

opaque type CIntegral[
    PlatformMapping[_ <: Platform] <: Int | Long | Short | Byte
] <: CVal[PlatformMapping] = CVal[PlatformMapping]

object CIntegral:
  trait Implementor extends CVal.Implementor:
    type Mapping[_ <: Platform] <: Int | Long | Short | Byte

    given cmath: CIntegralMath[CIntegral[Mapping]]

    def cintegral[P <: Platform](using P)(v: Mapping[P]): CIntegral[Mapping] =
      cval[P](v)

    given ([P <: Platform] => P ?=> Mapping[P] => CIntegral[Mapping]) =
      [P <: Platform] => (p: P) ?=> cintegral(_)

    extension [A <: CIntegral[Mapping]](a: A)
      def toInt(using r: Runtime) = cmath.toInt(a)
      def abs(using r: Runtime) = cmath.abs(a)
      def +(b: A)(using Runtime) = cmath.plus(a, b)

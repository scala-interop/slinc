package fr.hammons.slinc.experimental

import fr.hammons.slinc.Platform

opaque type CLong <: CIntegral[CLong.Mapping] = CIntegral[CLong.Mapping]

object CLong extends CIntegral.Implementor:
  type Mapping[A <: Platform] <: Int | Short | Long | Byte = A match
    case Platform.LinuxX64 | Platform.MacX64 => Long
    case Platform.WinX64                     => Int

  given cmath: CIntegralMath[CLong] =
    CIntegralMath.derive
  // given cord: Ordering[CLong] = ???
  def apply(using r: fr.hammons.slinc.Runtime)(i: Int): CLong =
    r.platform match
      case given Platform.WinX64 =>
        cintegral(i)
      case given (Platform.LinuxX64 | Platform.MacX64) =>
        cintegral(Long.box(i))

package fr.hammons.slinc.experimental

import fr.hammons.slinc.Platform
import fr.hammons.slinc.experimental.CIntegral.Implementor

opaque type CLong <: CIntegral[CLong.Mapping] = CIntegral[CLong.Mapping]

object CLong extends CIntegral.Implementor:
  type Mapping[A <: Platform] <: CIntegral.IntegralTypes = A match
    case Platform.LinuxX64 | Platform.MacX64 => Long
    case Platform.WinX64                     => Int

  type Me = CLong
  override def apply[P <: Platform](using P)(a: Mapping[P]): CLong = cintegral(
    a
  )

  given cmath: CMath =
    CIntegralMath.derive
  // given cord: Ordering[CLong] = ???
  def apply(using r: fr.hammons.slinc.Runtime)(i: Int): CLong =
    r.platform match
      case given Platform.WinX64 =>
        cintegral(i)
      case given (Platform.LinuxX64 | Platform.MacX64) =>
        cintegral(i)

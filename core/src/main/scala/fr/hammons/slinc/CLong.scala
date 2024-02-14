package fr.hammons.slinc

import fr.hammons.slinc.internal.CIntegral
import fr.hammons.slinc.internal.LightOption

import annotation.switch
import fr.hammons.slinc.internal.TypeRelation

opaque type CLong <: CIntegral = CIntegral

object CLong:
  given TypeRelation[Platform.WinX64.type, CLong] with {
    type Real = Int
  }
  given TypeRelation[Platform.LinuxX64.type | Platform.MacX64.type, CLong]
  with {
    type Real = Long
  }

  // given (using Runtime): Ordered[CLong] = CIntegral.orderingDerivation[CLong]

  given (using Runtime): Numeric[CLong] =
    CIntegral.numericDerivation[CLong]

  def apply(i: Int)(using r: Runtime): CLong =
    r.platform.match
      case given Platform.WinX64.type => CIntegral(i)
      case given (Platform.LinuxX64.type | Platform.MacX64.type) =>
        CIntegral(i.toLong)

  def apply(using Platform.LinuxX64.type | Platform.MacX64.type)(
      l: Long
  ): CLong = CIntegral(l)
  // def apply(l: Long): LightOption[CLong] = if l <= Int.MaxValue && l >= Int.MinValue then LightOption(CIntegral(l)) else LightOption.None
  def apply(l: Long)(using r: Runtime): LightOption[CLong] =
    println(l)
    println(r)
    (r.platform: @switch) match
      case given Platform.WinX64.type =>
        if l <= Int.MaxValue && l >= Int.MinValue then
          LightOption(CIntegral(l.toInt))
        else LightOption.None
      case given (Platform.LinuxX64.type | Platform.MacX64.type) =>
        LightOption(CIntegral(l))

  // extension (clong1: CLong)
  //   def +(clong2: CLong)(using r: Runtime): CLong =
  //     (r.platform: @switch()).match
  //       case given Platform.WinX64.type =>
  //         CLong(clong1.certainAs(classOf[Int]) + clong2.certainAs(classOf[Int]))
  //       case given (Platform.LinuxX64.type | Platform.MacX64.type) =>
  //         println("here i am")
  //         CLong.certain(
  //           clong1.certainAs(classOf[Long]) + clong2.certainAs(classOf[Long])
  //         )

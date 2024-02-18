package fr.hammons.slinc.experimental

import fr.hammons.slinc.Runtime
import fr.hammons.slinc.Platform
import compiletime.{summonInline, uninitialized, summonFrom, asMatchable}
import fr.hammons.slinc.Platform.WinX64
import scala.annotation.switch
import fr.hammons.slinc.experimental.CIntegral.GetLesserIntegrals
import fr.hammons.slinc.experimental.CIntegral.Minima
import fr.hammons.slinc.experimental.CIntegral.IntegralTypes
import fr.hammons.slinc.experimental.CIntegral.MinimaOrLess

sealed trait CIntegralMath[A] {
  type MathMapping[_ <: Platform] <: CIntegral.IntegralTypes
  def fromMinimaOrLess(
      v: CIntegral.Minima[MathMapping] |
        CIntegral.GetLesserIntegrals[CIntegral.Minima[MathMapping]]
  )(using Runtime, MinimaOrLess[MathMapping] <:< CIntegral.IntegralTypes): A
  def lesser[P <: Platform](using P)(
      v: CIntegral.GetLesserIntegrals[MathMapping[P]]
  ): A
  def abs(x: A)(using Runtime): A
  def compare(x: A, y: A): Int
  def equiv(x: A, y: A)(using Runtime): Boolean
  def gt(x: A, y: A): Boolean
  def gteq(x: A, y: A): Boolean
  def toInt(x: A)(using Runtime): Int
  def toFloat(x: A)(using Runtime): Float
  def plus(x: A, y: A)(using Runtime): A
  def toIntegral(x: A)(using Runtime): CIntegral.IntegralTypes
}

object CIntegralMath:
  private[experimental] inline def integralTypesToMapping[
      P <: Platform,
      Mapping[
          _ <: Platform
      ] <: CIntegral.IntegralTypes
  ](using P)(
      v: CIntegral.IntegralTypes
  ): Mapping[P] =
    summonFrom {
      case eq: (Byte =:= Mapping[P]) =>
        v match
          case b: Byte  => eq(b)
          case _: Short => throw MappingExceededError("Short", "Byte")
          case _: Int   => throw MappingExceededError("Int", "Byte")
          case _: Long  => throw MappingExceededError("Long", "Byte")
      case eq: (Short =:= Mapping[P]) =>
        v match
          case b: Byte  => eq(b.toShort)
          case s: Short => eq(s)
          case _: Int   => throw MappingExceededError("Int", "Short")
          case _: Long  => throw MappingExceededError("Long", "Short")
      case eq: (Int =:= Mapping[P]) =>
        v match
          case b: Byte  => eq(b.toInt)
          case s: Short => eq(s.toInt)
          case i: Int   => eq(i)
          case _: Long  => throw MappingExceededError("Long", "Int")
      case eq: (Long =:= Mapping[P]) =>
        v match
          case b: Byte  => eq(b.toLong)
          case s: Short => eq(s.toLong)
          case i: Int   => eq(i.toLong)
          case l: Long  => eq(l)
    }

  inline def derive[
      PlatformMapping[_ <: Platform] <: CIntegral.IntegralTypes,
      A <: CIntegral[PlatformMapping]
  ](using
      _aMaker: [P <: Platform] => P ?=> PlatformMapping[P] => A
  ): CIntegralMath[A] { type MathMapping[P <: Platform] = PlatformMapping[P] } =
    new CIntegralMath[A] {
      type MathMapping[P <: Platform] = PlatformMapping[P]
      override def lesser[P <: Platform](using p: P)(
          v: GetLesserIntegrals[PlatformMapping[P]]
      ): A = haveIntegralTypesConverter(p)(
        [P <: Platform] => fn => fn(v)
      )

      override def fromMinimaOrLess(
          v: MinimaOrLess[MathMapping]
      )(using
          r: Runtime,
          ev: MinimaOrLess[MathMapping] <:< CIntegral.IntegralTypes
      ): A = haveIntegralTypesConverter(r.platform)(
        [P <: Platform] => fn => fn(v)
      )

      def toIntegral(x: A)(using Runtime): CIntegral.IntegralTypes =
        integralUsage[CIntegral.IntegralTypes](
          [P <: Platform] => p ?=> (_, extractor) => extractor(x)
        )
      override def equiv(x: A, y: A)(using Runtime): Boolean = integralUsage(
        [P <: Platform] =>
          p ?=> (int, extractor) => int.equiv(extractor(x), extractor(y))
      )

      override def gt(x: A, y: A): Boolean = ???

      override def gteq(x: A, y: A): Boolean = ???

      override def compare(x: A, y: A): Int = ???

      final override def plus(x: A, y: A)(using r: Runtime): A = integralUsage(
        [P <: Platform] =>
          p ?=>
            (int, extractor) => _aMaker(int.plus(extractor(x), extractor(y)))
      )

      def integralUsage[B](
          fn: [P <: Platform] => P ?=> (
              Integral[PlatformMapping[P]],
              CIntegral[PlatformMapping] => PlatformMapping[P]
          ) => B
      )(using r: Runtime): B =
        (r.platform: @switch).match
          case given Platform.WinX64 =>
            fn(summonInline, _.extract)
          case given Platform.LinuxX64 => fn(summonInline, _.extract)
          case given Platform.MacX64   => fn(summonInline, _.extract)

      def haveIntegralTypesConverter(p: Platform)(
          fn: [P <: Platform] => (
              IntegralTypes => MathMapping[P]
          ) => MathMapping[P]
      ): A =
        p match
          case given Platform.WinX64 =>
            _aMaker(fn(integralTypesToMapping[Platform.WinX64, MathMapping](_)))
          case given Platform.LinuxX64 =>
            _aMaker(
              fn(integralTypesToMapping[Platform.LinuxX64, MathMapping](_))
            )
          case given Platform.MacX64 =>
            _aMaker(fn(integralTypesToMapping[Platform.MacX64, MathMapping](_)))

      def abs(x: A)(using r: Runtime): A = integralUsage(
        [P <: Platform] =>
          p ?=> (int, extractor) => _aMaker(int.abs(extractor(x)))
      )
      def toInt(x: A)(using r: Runtime): Int = integralUsage(
        [P <: Platform] => p ?=> (int, extractor) => int.toInt(extractor(x))
      )
      def toFloat(x: A)(using r: Runtime): Float = integralUsage(
        [P <: Platform] => p ?=> (int, extractor) => int.toFloat(extractor(x))
      )
    }

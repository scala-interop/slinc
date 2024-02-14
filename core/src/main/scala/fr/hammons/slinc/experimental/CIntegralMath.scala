package fr.hammons.slinc.experimental

import fr.hammons.slinc.Runtime
import fr.hammons.slinc.Platform
import compiletime.{summonInline, uninitialized}
import fr.hammons.slinc.Platform.WinX64
import scala.annotation.switch
import java.util.concurrent.atomic.AtomicReference

sealed trait CIntegralMath[A] {
  // type Mapping[_ <: Platform] <: Int | Short | Byte | Long
  // val st: (A <:< CIntegral[Mapping])
  // val integralUsage: [B] => (
  //     [P <: Platform] => P ?=> Integral[Mapping[P]] => B
  // ) => Runtime ?=> B
  // val aMaker: [P <: Platform] => P ?=> Mapping[P] => A

  def abs(x: A)(using Runtime): A
  def compare(x: A, y: A): Int
  def equiv(x: A, y: A): Boolean
  def gt(x: A, y: A): Boolean
  def gteq(x: A, y: A): Boolean
  def toInt(x: A)(using Runtime): Int
  def toFloat(x: A)(using Runtime): Float
  def plus(x: A, y: A)(using Runtime): A // integralUsage(
  //   [P <: Platform] =>
  //     p ?=> int => aMaker(int.plus(st(x).extract, st(y).extract))
  // )
}

object CIntegralMath:
  sealed class CIntegralMathImpl[
      PlatformMapping[_ <: Platform] <: Int | Short | Byte | Long,
      A <: CIntegral[PlatformMapping]
  ] private (
      val integralUsage: [B] => (
          [P <: Platform] => P ?=> Integral[PlatformMapping[P]] => B
      ) => Runtime ?=> B,
      val aMaker: [P <: Platform] => P ?=> PlatformMapping[P] => A
  ) extends CIntegralMath[A]:

    final def plus(x: A, y: A)(using Runtime): A = integralUsage(
      [P <: Platform] => p ?=> int => aMaker(int.plus(x.extract, y.extract))
    )

    override def equiv(x: A, y: A): Boolean = ???

    override def abs(x: A)(using r: Runtime): A = ???

    override def toFloat(x: A)(using r: Runtime): Float = ???

    override def gteq(x: A, y: A): Boolean = ???

    override def toInt(x: A)(using r: Runtime): Int = ???

    override def gt(x: A, y: A): Boolean = ???

    override def compare(x: A, y: A): Int = ???

    type Mapping[P <: Platform] = PlatformMapping[P]

  object CIntegralMathImpl:
    private[CIntegralMath] def apply[
        PlatformMapping[_ <: Platform] <: Int | Short | Byte | Long,
        A <: CIntegral[PlatformMapping]
    ](
        integralUsage: [B] => (
            [P <: Platform] => P ?=> Integral[PlatformMapping[P]] => B
        ) => Runtime ?=> B,
        aMaker: [P <: Platform] => P ?=> PlatformMapping[P] => A
    ): CIntegralMath[A] = new CIntegralMathImpl(integralUsage, aMaker)

  inline def derive[
      PlatformMapping[_ <: Platform] <: Int | Short | Byte | Long,
      A <: CIntegral[PlatformMapping]
  ](using
      _aMaker: [P <: Platform] => P ?=> PlatformMapping[P] => A
  ): CIntegralMath[A] =
    // CIntegralMathImpl(
    //   [B] =>
    //     (fn: [P <: Platform] => P ?=> Integral[PlatformMapping[P]] => B) =>
    //       r ?=>
    //         r.platform.match
    //           case given Platform.WinX64   => fn(summonInline)
    //           case given Platform.LinuxX64 => fn(summonInline)
    //           case given Platform.MacX64   => fn(summonInline)
    //   ,
    //   _aMaker
    // )
    new CIntegralMath[A] {

      override def equiv(x: A, y: A): Boolean = ???

      override def gt(x: A, y: A): Boolean = ???

      override def gteq(x: A, y: A): Boolean = ???

      override def compare(x: A, y: A): Int = ???

      final override def plus(x: A, y: A)(using r: Runtime): A = integralUsage(
        [P <: Platform] => p ?=> int => _aMaker(int.plus(x.extract, y.extract))
      )

      def integralUsage[B](
          fn: [P <: Platform] => P ?=> Integral[PlatformMapping[P]] => B
      )(using r: Runtime): B =
        (r.platform: @switch).match
          case given Platform.WinX64 =>
            fn(summonInline)
          case given Platform.LinuxX64 => fn(summonInline)
          case given Platform.MacX64   => fn(summonInline)

      def abs(x: A)(using r: Runtime): A = integralUsage(
        [P <: Platform] =>
          (p: P) ?=>
            (int: Integral[PlatformMapping[P]]) => _aMaker(int.abs(x.extract))
      )
      def toInt(x: A)(using r: Runtime): Int = integralUsage(
        [P <: Platform] =>
          (p: P) ?=> (int: Integral[PlatformMapping[P]]) => int.toInt(x.extract)
      )
      def toFloat(x: A)(using r: Runtime): Float = integralUsage(
        [P <: Platform] =>
          (p: P) ?=>
            (int: Integral[PlatformMapping[P]]) => int.toFloat(x.extract)
      )
    }

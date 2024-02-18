package fr.hammons.slinc.experimental

import fr.hammons.slinc.Platform
import fr.hammons.slinc.Runtime
import compiletime.{summonFrom, asMatchable}

opaque type CIntegral[
    PlatformMapping[_ <: Platform] <: CIntegral.IntegralTypes
] <: CVal[PlatformMapping] = CVal[PlatformMapping]

object CIntegral:
  type IntegralTypes = Byte | Short | Int | Long

  type LesserIntegrals[I, J <: Nothing] <: Int | Short | Byte = I match
    case Long  => LesserIntegrals[Int, J | Int]
    case Int   => LesserIntegrals[Short, J | Short]
    case Short => LesserIntegrals[Byte, J | Byte]
    case Byte  => J

  type GetLesserIntegrals[I] = LesserIntegrals[I, Nothing]
  type Lesser[
      A <: CIntegral.IntegralTypes,
      B <: CIntegral.IntegralTypes
  ] = A match
    case Byte => Byte
    case Short =>
      B match
        case Byte => Byte
        case _    => Short
    case Int =>
      B match
        case Byte  => Byte
        case Short => Short
        case _     => Int
    case Long =>
      B match
        case Byte  => Byte
        case Short => Short
        case Int   => Int
        case _     => Long

  type MinimaHelper[
      Minimal <: IntegralTypes,
      T <: Tuple,
      Mapping[_ <: Platform] <: IntegralTypes
  ] <: IntegralTypes = T match
    case head *: tail =>
      MinimaHelper[Lesser[Minimal, Mapping[head]], tail, Mapping]
    case EmptyTuple => Minimal

  type Minima[
      Mapping[_ <: Platform] <: IntegralTypes
  ] = MinimaHelper[Long, Platform.AllSet, Mapping]

  type MinimaOrLess[
      Mapping[_ <: Platform] <: IntegralTypes
  ] = Minima[Mapping] | GetLesserIntegrals[Minima[Mapping]]

  trait Implementor extends CVal.Implementor:
    type Mapping[_ <: Platform] <: IntegralTypes
    type Me <: CIntegral[Mapping]

    def apply[P <: Platform](using P)(a: Mapping[P]): Me
    inline def lesser[P <: Platform](using P)(
        a: GetLesserIntegrals[Mapping[P]]
    ): Me =
      cmath.lesser(a)

    def minimaOrLess(using Runtime, MinimaOrLess[Mapping] <:< IntegralTypes)(
        minima: MinimaOrLess[Mapping]
    ): Me = cmath.fromMinimaOrLess(minima)

    type CMath =
      (CIntegralMath[Me] { type MathMapping[P <: Platform] = Mapping[P] })
    given cmath: CMath

    def cintegral[P <: Platform](using P)(v: Mapping[P]): CIntegral[Mapping] =
      cval[P](v)

    given ([P <: Platform] => P ?=> Mapping[P] => CIntegral[Mapping]) =
      [P <: Platform] => (p: P) ?=> apply(_)

    extension (a: Me)
      def toInt(using r: Runtime) = cmath.toInt(a)
      def abs(using r: Runtime) = cmath.abs(a)
      def +(b: Me)(using Runtime) = cmath.plus(a, b)
      def toIntegral(using Runtime) = cmath.toIntegral(a)

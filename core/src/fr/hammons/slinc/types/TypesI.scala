package fr.hammons.slinc.types

import fr.hammons.slinc.*
import fr.hammons.slinc.container.*

class TypesI protected[slinc] (
    protected val platformSpecific: TypesI.PlatformSpecific
):
  /** Fixed-size type referencing an 8-bit integer. Equivalent to [[Byte]] */
  type Int8 = Byte

  /** Fixed-size type referencing a 16-bit integer. Equivalent to [[Short]] */
  type Int16 = Short

  /** Fixed-size type referencing a 32-bit integer. Equivalent to [[Int]] */
  type Int32 = Int

  /** Fixed-size type referencing a 64-bit integer. Equivalent to [[Long]] */
  type Int64 = Long

  type CChar = Int8
  type CShort = Int16
  type CInt = Int32
  type CLongLong = Int64
  type CFloat = Float
  type CDouble = Double

  /** Type representing C's long type
    * @note
    *   this type is unknown until runtime
    */
  type CLong = platformSpecific.CLong
  type SizeT = platformSpecific.SizeT
  type TimeT = platformSpecific.TimeT
  given platformSpecific.CLongProof = platformSpecific.cLongProof
  given platformSpecific.SizeTProof = platformSpecific.sizeTProof
  given platformSpecific.TimeTProof = platformSpecific.timeTProof

  type ConversionPair[A, B] = (
      ContextProof[Conversion[A, *] *::: End, B],
      ContextProof[Conversion[*, A] *::: End, B]
  )

  type ConversionPair2[A, B, C] = Conversion[A, B] ?=> Conversion[B, A] ?=> C

  inline def proveEqFor[A, B, C](
      cfn: Conversion[A, B] ?=> Conversion[B, A] ?=> C
  ): C =
    given Conversion[A, B] = _.asInstanceOf[B]
    given Conversion[B, A] = _.asInstanceOf[A]
    cfn

  class AssertionZone[A, B](valid: Boolean)(using
      Conversion[A, B],
      Conversion[B, A]
  ):
    def apply[R](cfn: Conversion[A, B] ?=> Conversion[B, A] ?=> R): Option[R] =
      if valid then Some(cfn) else None

  def platformFocus[Platform <: HostDependentTypes & Singleton, B](p: Platform)(
      cfn: ConversionPair2[
        CLong,
        p.CLong,
        ConversionPair2[SizeT, p.SizeT, ConversionPair2[TimeT, p.TimeT, B]]
      ]
  ): Option[B] =
    if platformSpecific.hostDependentTypes.eq(p) then
      Some(proveEqFor[p.CLong, CLong, B] {
        proveEqFor[p.SizeT, SizeT, B] {
          proveEqFor[p.TimeT, TimeT, B] {
            cfn
          }
        }
      })
    else None

object TypesI:
  type :->[A] = [B] =>> Convertible[B, A]
  type <-:[A] = [B] =>> Convertible[A, B]
  type :?->[A] = [B] =>> PotentiallyConvertible[B, A]
  type <-?:[A] = [B] =>> PotentiallyConvertible[A, B]
  type StandardCapabilities = DescriptorOf *::: End

  trait PlatformSpecific extends HostDependentTypes:
    val hostDependentTypes: HostDependentTypes & Singleton

    // Type representing C's long type
    type CLongProof = ContextProof[
      :->[Long] *::: <-:[Int] *::: <-?:[Long] *::: :?->[Int] *:::
        StandardCapabilities,
      CLong
    ]

    given MethodCompatible[CLong] with {}

    given cLongProof: CLongProof

    type SizeTProof = ContextProof[
      :->[Long] *::: <-:[Int] *::: <-?:[Long] *::: :?->[Int] *:::
        StandardCapabilities,
      SizeT
    ]
    val sizeTProof: SizeTProof

    given MethodCompatible[SizeT] with {}

    type TimeTProof = ContextProof[
      :?->[Int] *::: <-?:[Int] *::: :?->[Long] *::: <-?:[Long] *:::
        StandardCapabilities,
      TimeT
    ]
    val timeTProof: TimeTProof

    given MethodCompatible[TimeT] with {}

  protected[slinc] val platformTypes: TypesI =
    val platform = (arch, os) match
      case (Arch.X64, OS.Linux)   => types.x64.Linux()
      case (Arch.X64, OS.Windows) => types.x64.Windows()
      case (Arch.X64, OS.Darwin)  => types.x64.Mac()
      case _                      => types.x64.Mac()
    TypesI(platform)

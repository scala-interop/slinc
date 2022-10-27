package fr.hammons.slinc.types

import scala.annotation.targetName
import scala.compiletime.constValue
import fr.hammons.slinc.*
import fr.hammons.slinc.container.*

class TypesI protected[slinc] (protected val platformSpecific: TypesI.PlatformSpecific):
  type Int8 = Byte
  type Int16 = Short
  type Int32 = Int
  type Int64 = Long
  type UInt64 = Long

  type CChar = Int8
  type CShort = Int16
  type CInt = Int32
  type CLongLong = Int64
  /** Type representing C's long type
   * @note this type is unknown until runtime
   */
  type CLong = platformSpecific.CLong
  type SizeT = platformSpecific.SizeT
  given platformSpecific.CLongProof = platformSpecific.cLongProof
  given platformSpecific.SizeTProof = platformSpecific.sizeTProof

object TypesI:
  type :->[A] = [B] =>> Convertible[B, A]
  type <-:[A] = [B] =>> Convertible[A, B]
  type :?->[A] = [B] =>> PotentiallyConvertible[B, A]
  type <-?:[A] = [B] =>> PotentiallyConvertible[A, B]
  type StandardCapabilities = LayoutOf *::: NativeInCompatible *:::
      NativeOutCompatible *::: Send *::: Receive *::: End


  private[slinc] trait PlatformSpecific:
    // Type representing C's long type
    type CLong
    type CLongProof = ContextProof[
      :->[Long] *::: <-:[Int] *::: <-?:[Long] *::: :?->[Int] *:::
        StandardCapabilities,
      CLong
    ]

    given cLongProof: CLongProof
    type SizeT

    type SizeTProof = ContextProof[
      :->[Long] *::: <-:[Int] *::: <-?:[Long] *::: :?->[Int] *:::
        StandardCapabilities,
      SizeT
    ]
    given sizeTProof: SizeTProof

  protected[slinc] val platformTypes: LayoutI => TypesI = layout =>
    val platform = (arch, os) match
      case (Arch.X64, OS.Linux)   => types.x64.Linux(layout)
      case (Arch.X64, OS.Windows) => types.x64.Windows(layout)
      case (Arch.X64, OS.Darwin)  => types.x64.Mac(layout)
      case _                      => types.x64.Mac(layout)
    TypesI(platform)

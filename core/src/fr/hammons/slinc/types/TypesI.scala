package fr.hammons.slinc.types

import scala.annotation.targetName
import scala.compiletime.constValue
import fr.hammons.slinc.*
import fr.hammons.slinc.container.*

class TypesI protected[slinc] (protected val platformSpecific: TypesI.PlatformSpecific):
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

package fr.hammons.slinc

import scala.annotation.targetName
import scala.compiletime.constValue

class TypesI(protected val platformSpecific: TypesI.PlatformSpecific):
  type Int8 = Byte
  type Int16 = Short
  type Int32 = Int
  type Int64 = Long
  type UInt64 = Long

  type CChar = Int8
  type CShort = Int16
  type CInt = Int32
  type CLongLong = Int64
  export platformSpecific.*
  export platformSpecific.{
    given CompatibilityProof[LayoutOf *::: NativeInCompatible *::: End, SizeT],
    given
  }

trait CompatibilityProof[A, Name]:
  given aLayout: LayoutOf[A]
  given aReceive: Receive[A]
  given aSend: Send[A]
  given aNativeInCompatible: NativeInCompatible[A]
  given aNativeOutCompatible: NativeOutCompatible[A]
object TypesI:
  trait PlatformSpecific:
    type ConvertibleTo[A] = Convertible[*, A]
    type ConvertibleFrom[A] = Convertible[A, *]

    type :->[A] = [B] =>> Convertible[B, A]
    type <-:[A] = [B] =>> Convertible[A, B]
    type :?->[A] = [B] =>> PotentiallyConvertible[B, A]
    type <-?:[A] = [B] =>> PotentiallyConvertible[A, B]
    type PotentiallyConvertibleTo[A] = PotentiallyConvertible[*, A]
    type PotentiallyConvertibleFrom[A] = PotentiallyConvertible[A, *]
    type StandardCapabilities = LayoutOf *::: NativeInCompatible *:::
      NativeOutCompatible *::: Send *::: Receive *::: End

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

  val platformTypes: LayoutI => TypesI = layout =>
    val platform = (arch, os) match
      case (Arch.X64, OS.Linux)   => x64.Linux(layout)
      case (Arch.X64, OS.Windows) => x64.Windows(layout)
      case (Arch.X64, OS.Darwin)  => x64.Mac(layout)
      case _                      => x64.Mac(layout)
    TypesI(platform)

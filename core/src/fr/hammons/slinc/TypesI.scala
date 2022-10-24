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
    type StandardCapabilities = LayoutOf *::: NativeInCompatible *:::
      NativeOutCompatible *::: Send *::: Receive *::: End

    type CLong
    given cLongProof
        : ContextProof[LayoutOf *::: NativeInCompatible *::: End, CLong]
    extension (l: Int) def toCLong: CLong

    extension (l: Long) def toCLong: Option[CLong]

    type SizeT
    given sizeTProof: ContextProof[StandardCapabilities, SizeT]

    extension (l: Int) def toSizeT: SizeT
    extension (l: Long) def toSizeT: Option[SizeT]

    // type SizeT
    // given sizeTLayout: LayoutOf[SizeT]

  val platformTypes: LayoutI => TypesI = layout =>
    val platform = (arch, os) match
      case (Arch.X64, OS.Linux)   => x64.Linux(layout)
      case (Arch.X64, OS.Windows) => x64.Windows(layout)
      case (Arch.X64, OS.Darwin) =>  x64.Mac(layout)
      case _                      => x64.Mac(layout)
    TypesI(platform)

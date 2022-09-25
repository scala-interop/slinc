package fr.hammons.slinc

class TypesI(protected val platformSpecific: TypesI.PlatformSpecific):
  type Int8 = Byte
  type Int16 = Short
  type Int32 = Int
  type Int64 = Long
  type CChar = Int8
  type CShort = Int16
  type CInt = Int32
  type CLongLong = Int64
  export platformSpecific.{*, given}

object TypesI:
  trait PlatformSpecific:
    type CLong
    given cLongLayout: LayoutOf[CLong]
    given cLongReceive: Receive[CLong]
    given cLongSend: Send[CLong]
    given cLongNativeInCompatible: NativeInCompatible[CLong]
    given cLongNativeOutCompatible: NativeOutCompatible[CLong]
    extension (l: Int) def toCLong: CLong

    extension (l: Long) def toCLong: Option[CLong]

  val platformTypes: LayoutI => TypesI = layout =>
    TypesI(PlatformX64Linux(layout))

  class PlatformX64Linux(layoutI: LayoutI) extends PlatformSpecific:
    type CLong = Long
    given cLongLayout: LayoutOf[CLong] = layoutI.given_LayoutOf_Long
    given cLongReceive: Receive[CLong] = Receive.given_Receive_Long
    given cLongSend: Send[CLong] = Send.given_Send_Long
    given cLongNativeInCompatible: NativeInCompatible[CLong] =
      NativeInCompatible.given_NativeInCompatible_Long
    given cLongNativeOutCompatible: NativeOutCompatible[CLong] =
      NativeOutCompatible.given_NativeOutCompatible_Long

    extension (l: Int) def toCLong: CLong = l
    extension (l: Long) def toCLong: Option[CLong] = Some(l)

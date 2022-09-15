package fr.hammons.sffi

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
    

  class PlatformX64Linux(protected val layoutI: LayoutI) extends PlatformSpecific:
    type CLong = Long
    given cLongLayout: LayoutOf[CLong] = layoutI.longLayout
  
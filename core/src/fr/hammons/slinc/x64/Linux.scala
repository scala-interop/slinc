package fr.hammons.slinc.x64

import fr.hammons.slinc.TypesI
import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.LayoutOf
import fr.hammons.slinc.*:::
import fr.hammons.slinc.ContextProof
import fr.hammons.slinc.End
import fr.hammons.slinc.NativeInCompatible
import fr.hammons.slinc.Send
import fr.hammons.slinc.Receive
import scala.annotation.targetName

class Linux(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given 


  type CLong = Long
  given cLongProof: ContextProof[:->[Long] *::: <-:[Int] *::: <-?:[Long] *::: :?->[Int] *::: StandardCapabilities, CLong] = ContextProof()

  class CLongDef():
    opaque type CLong = Long


    extension (l: Long) def toCLong: Option[CLong] = Some(l)

    extension (l: Int) def toCLong: CLong = l

    extension (c: CLong) @targetName("CLongToInt") def toInt = c.toInt 
  
    extension (c: CLong) @targetName("CLongToLong") def toLong: Option[Long] = Some(c)
  
  type SizeT = Long

  override given sizeTProof: ContextProof[StandardCapabilities, SizeT] = ContextProof()

  extension (l: Long) override def toSizeT: Option[SizeT] = Some(l)

  extension (l: Int) override def toSizeT: SizeT = l
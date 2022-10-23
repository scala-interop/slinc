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

class Linux(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given 
  type SizeT = Long
  override given cLongProof: ContextProof[LayoutOf *::: NativeInCompatible *::: End, Long] = ContextProof()

  extension (l: Long) override def toCLong: Option[CLong] = Some(l)

  extension (l: Int) override def toCLong: CLong = l

  override given sizeTProof: ContextProof[StandardCapabilities, SizeT] = ContextProof()

  extension (l: Long) override def toSizeT: Option[SizeT] = Some(l)

  extension (l: Int) override def toSizeT: SizeT = l

  type CLong = Long

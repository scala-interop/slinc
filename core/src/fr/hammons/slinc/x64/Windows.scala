package fr.hammons.slinc.x64

import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.TypesI
import fr.hammons.slinc.*:::
import fr.hammons.slinc.ContextProof
import fr.hammons.slinc.End
import fr.hammons.slinc.LayoutOf
import fr.hammons.slinc.NativeInCompatible
import fr.hammons.slinc.Send
import fr.hammons.slinc.Receive

class Windows(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given
  type CLong = Int
  given cLongProof: ContextProof[
    :->[Long] *::: <-:[Int] *::: <-?:[Long] *::: :?->[Int] *:::
      StandardCapabilities,
    CLong
  ] = ContextProof()

  type SizeT = Long
  override given sizeTProof: SizeTProof =
    ContextProof()

package fr.hammons.slinc.types.x64

import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.types.TypesI
import fr.hammons.slinc.container.ContextProof

private[slinc] class Mac(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given

  type CLong = Long
  override given cLongProof: CLongProof = ContextProof()

  type SizeT = Long
  override given sizeTProof: SizeTProof =
    ContextProof()

  type TimeT = Long
  override given timeTProof: TimeTProof = ContextProof()

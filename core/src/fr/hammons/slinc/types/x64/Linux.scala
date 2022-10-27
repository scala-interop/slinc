package fr.hammons.slinc.types.x64

import fr.hammons.slinc.types.TypesI
import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.container.ContextProof

private[types] class Linux(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given

  type CLong = Long
  given cLongProof: CLongProof = ContextProof()

  type SizeT = Long

  override given sizeTProof: SizeTProof =
    ContextProof()

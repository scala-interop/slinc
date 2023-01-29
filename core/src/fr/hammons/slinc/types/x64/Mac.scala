package fr.hammons.slinc.types.x64

import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.types.TypesI
import fr.hammons.slinc.container.ContextProof
import fr.hammons.slinc.types.HostDependentTypes

private[slinc] class Mac(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given

  val hostDependentTypes = Mac
  type CLong = Mac.CLong
  override given cLongProof: CLongProof = ContextProof()

  type SizeT = Mac.SizeT
  override val sizeTProof: SizeTProof =
    ContextProof()

  type TimeT = Mac.TimeT
  override val timeTProof: TimeTProof = ContextProof()

object Mac extends HostDependentTypes:
  type CLong = Long 
  type SizeT = Long
  type TimeT = Long
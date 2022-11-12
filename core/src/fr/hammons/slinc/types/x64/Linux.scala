package fr.hammons.slinc.types.x64

import fr.hammons.slinc.types.TypesI
import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.container.ContextProof
import fr.hammons.slinc.types.HostDependentTypes

private[types] class Linux(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given

  val hostDependentTypes = Linux

  type CLong = Linux.CLong
  given cLongProof: CLongProof = ContextProof()

  type SizeT = Linux.SizeT
  override given sizeTProof: SizeTProof =
    ContextProof()

  type TimeT = Linux.TimeT

  given timeTProof: TimeTProof = ContextProof()


object Linux extends HostDependentTypes:
  type TimeT = Long 
  type SizeT = Long 
  type CLong = Long

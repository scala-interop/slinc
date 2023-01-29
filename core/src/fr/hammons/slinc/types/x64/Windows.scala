package fr.hammons.slinc.types.x64

import fr.hammons.slinc.LayoutI
import fr.hammons.slinc.types.TypesI
import fr.hammons.slinc.container.ContextProof
import fr.hammons.slinc.types.HostDependentTypes

private[slinc] class Windows(layoutI: LayoutI) extends TypesI.PlatformSpecific:
  import layoutI.given

  val hostDependentTypes = Windows


  type CLong = Windows.CLong
  given cLongProof: CLongProof = ContextProof()

  type SizeT = Windows.SizeT
  override val sizeTProof: SizeTProof =
    ContextProof()

  type TimeT = Windows.TimeT 
  override val timeTProof: TimeTProof =
    ContextProof()

object Windows extends HostDependentTypes:
  type CLong = Int
  type SizeT = Long 
  type TimeT = Long
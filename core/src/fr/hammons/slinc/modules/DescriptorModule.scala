package fr.hammons.slinc.modules

import fr.hammons.slinc.Bytes
import fr.hammons.slinc.ForeignTypeDescriptor

/** A module used to perform Platform dependent work with a descriptor
  */
trait DescriptorModule:
  def memberOffsets(sd: List[ForeignTypeDescriptor]): IArray[Bytes]
  def sizeOf(td: ForeignTypeDescriptor): Bytes
  def alignmentOf(td: ForeignTypeDescriptor): Bytes
  def toCarrierType(td: ForeignTypeDescriptor): Class[?]

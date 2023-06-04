package fr.hammons.slinc.modules

import fr.hammons.slinc.{Bytes, TypeDescriptor}

/** A module used to perform Platform dependent work with a descriptor
  */
trait DescriptorModule:
  def memberOffsets(sd: List[TypeDescriptor]): IArray[Bytes]
  def sizeOf(td: TypeDescriptor): Bytes
  def alignmentOf(td: TypeDescriptor): Bytes
  def toCarrierType(td: TypeDescriptor): Class[?]

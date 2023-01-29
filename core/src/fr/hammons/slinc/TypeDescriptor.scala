package fr.hammons.slinc

import modules.DescriptorModule

/** Describes types used by C interop
  * 
  */
sealed trait TypeDescriptor:
  def size(using dm: DescriptorModule): Bytes = dm.sizeOf(this)
  def alignment(using dm: DescriptorModule): Bytes = dm.alignmentOf(this)
  def toCarrierType(using dm: DescriptorModule): Class[?] = dm.toCarrierType(this)

case object ByteDescriptor extends TypeDescriptor
case object ShortDescriptor extends TypeDescriptor
case object IntDescriptor extends TypeDescriptor
case object LongDescriptor extends TypeDescriptor

case object FloatDescriptor extends TypeDescriptor
case object DoubleDescriptor extends TypeDescriptor

case object PtrDescriptor extends TypeDescriptor

/** A descriptor of a member of a Struct
  * 
  *
  * @param descriptor The [[TypeDescriptor]] this member is representing
  * @param name The name of the member
  */
case class StructMemberDescriptor(descriptor: TypeDescriptor, name: String)

/** A descriptor for a C struct
  * 
  *
  * @param members The members of the struct
  * @param clazz The class that's an analog for this struct
  * @param transform A function for transforming a tuple into the analog 
  * of this Struct
  */
class StructDescriptor(
  val members: List[StructMemberDescriptor],
  val clazz: Class[?],
  val transform: Tuple => Product
) extends TypeDescriptor:
  def offsets(using d: DescriptorModule) = d.memberOffsets(this)

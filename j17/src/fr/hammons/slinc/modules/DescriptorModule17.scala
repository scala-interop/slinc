package fr.hammons.slinc.modules

import fr.hammons.slinc.*

import jdk.incubator.foreign.CLinker.{
  C_CHAR,
  C_SHORT,
  C_INT,
  C_DOUBLE,
  C_FLOAT,
  C_LONG_LONG,
  C_POINTER
}
import jdk.incubator.foreign.MemoryLayout
import scala.collection.concurrent.TrieMap

given DescriptorModule with
  val chm: TrieMap[StructDescriptor, StructLayout] = TrieMap.empty
  val offsets: TrieMap[StructDescriptor, IArray[Bytes]] = TrieMap.empty
      
  def genDataLayoutList(
      layouts: Seq[DataLayout],
      alignment: Long,
  ): Seq[DataLayout] =
    val (vector, currentLocation) = layouts.foldLeft(Seq.empty[DataLayout] -> 0L) {
      case ((vector, currentLocation), layout) => 
        val thisAlignment = layout.alignment.toLong
        val misalignment = currentLocation % thisAlignment
        val toAdd = 
          if misalignment == 0 then Seq(layout)
          else 
            val paddingNeeded = thisAlignment - misalignment

            Seq(
              PaddingLayout(Bytes(paddingNeeded), layout.byteOrder),
              layout
            )
        (vector ++ toAdd, currentLocation + toAdd.view.map(_.size.toLong).sum)
    }
    val misalignment = currentLocation % alignment
    vector ++ (
      if misalignment != 0 then 
        Seq(
          PaddingLayout(
            Bytes(alignment - misalignment),
            ByteOrder.HostDefault
          )
        )
      else Seq.empty
    )

  override def sizeOf(td: TypeDescriptor): Bytes = td match
    case ByteDescriptor   => Bytes(1)
    case ShortDescriptor  => Bytes(2)
    case IntDescriptor    => Bytes(4)
    case LongDescriptor   => Bytes(8)
    case FloatDescriptor  => Bytes(4)
    case DoubleDescriptor => Bytes(8)
    case PtrDescriptor    => toDataLayout(PtrDescriptor).size
    case sd: StructDescriptor =>
      toStructLayout(sd).size

  override def alignmentOf(td: TypeDescriptor): Bytes = td match
    case s: StructDescriptor =>
      Bytes(s.members.view.map(_.descriptor).map(alignmentOf).map(_.toLong).max)
    case _ => sizeOf(td)

  override def memberOffsets(sd: StructDescriptor): IArray[Bytes] =
    offsets.getOrElseUpdate(
      sd, {
        val ll = genDataLayoutList(
          sd.members.map(toDataLayout),
          sd.members.view.map(_.descriptor).map(alignmentOf).map(_.toLong).max,
        )
        IArray.from(
          ll match 
            case head :: next => 
              next.foldLeft(Seq(Bytes(0)) -> head.size){
                case ((offsets, lastSize), layout) =>
                  val newSize = lastSize + layout.size
                  val newOffsets = if layout.name.isDefined then 
                    offsets :+ lastSize
                  else offsets
                  (newOffsets, newSize)
              }._1
            case _ => 
              Seq.empty
        )
      }
    )

  override def toDataLayout(td: TypeDescriptor): DataLayout = td match
    case ByteDescriptor      => LayoutI17.byteLayout
    case ShortDescriptor     => LayoutI17.shortLayout
    case IntDescriptor       => LayoutI17.intLayout
    case LongDescriptor      => LayoutI17.longLayout
    case FloatDescriptor     => LayoutI17.floatLayout
    case DoubleDescriptor    => LayoutI17.doubleLayout
    case PtrDescriptor       => LayoutI17.pointerLayout
    case s: StructDescriptor => toStructLayout(s)

  def toDataLayout(std: StructMemberDescriptor): DataLayout =
    toDataLayout(std.descriptor).withName(std.name)

  override def toStructLayout(sd: StructDescriptor): StructLayout =
    val originalMembers = sd.members.map(toDataLayout)
    val alignment = alignmentOf(sd)
    val offsets = memberOffsets(sd)
    val structMembers =
      genDataLayoutList(originalMembers, alignment.toLong).foldLeft((Vector.empty[StructMember], 0)){
        case ((vector, offsetsIndex), dataLayout) => 
          if dataLayout.name.isDefined then 
            (vector :+ StructMember(dataLayout, dataLayout.name, offsets(offsetsIndex)), offsetsIndex+1)
          else 
            (vector :+ StructMember(dataLayout, None, Bytes(0)), offsetsIndex)
      }._1
    StructLayout(
      if sd.clazz.getCanonicalName() != null then
        Some(sd.clazz.getCanonicalName().nn)
      else None,
      Bytes(structMembers.view.map(_.layout.size.toLong).sum),
      Bytes(structMembers.view.map(_.layout.alignment.toLong).max),
      ByteOrder.HostDefault,
      sd.transform,
      sd.clazz,
      structMembers
    )

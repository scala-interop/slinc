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
import jdk.incubator.foreign.{
  MemoryLayout,
  MemoryAddress,
  MemorySegment,
  GroupLayout,
  CLinker
}
import scala.collection.concurrent.TrieMap

given descriptorModule17: DescriptorModule with
  val chm: TrieMap[StructDescriptor, GroupLayout] = TrieMap.empty
  val offsets: TrieMap[List[TypeDescriptor], IArray[Bytes]] = TrieMap.empty

  def toCarrierType(td: TypeDescriptor): Class[?] = td match
    case ByteDescriptor         => classOf[Byte]
    case ShortDescriptor        => classOf[Short]
    case IntDescriptor          => classOf[Int]
    case LongDescriptor         => classOf[Long]
    case FloatDescriptor        => classOf[Float]
    case DoubleDescriptor       => classOf[Double]
    case PtrDescriptor          => classOf[MemoryAddress]
    case _: StructDescriptor    => classOf[MemorySegment]
    case VaListDescriptor       => classOf[MemoryAddress]
    case ad: AliasDescriptor[?] => toCarrierType(ad.real)
    case ud: CUnionDescriptor   => classOf[MemorySegment]

  def genLayoutList(
      layouts: Seq[MemoryLayout],
      alignment: Bytes
  ): Seq[MemoryLayout] =
    val (vector, currentLocation) =
      layouts.foldLeft(Seq.empty[MemoryLayout] -> Bytes(0L)) {
        case ((vector, currentLocation), layout) =>
          val thisAlignment = Bytes(layout.byteAlignment())
          val misalignment = currentLocation % thisAlignment
          val toAdd =
            if misalignment == Bytes(0) then Seq(layout)
            else
              val paddingNeeded = thisAlignment - misalignment

              Seq(
                MemoryLayout.paddingLayout(paddingNeeded.toBits).nn,
                layout
              )
          (
            vector ++ toAdd,
            currentLocation + Bytes(toAdd.view.map(_.byteSize()).sum)
          )
      }
    val misalignment = currentLocation % alignment
    vector ++ (
      if misalignment != Bytes(0) then
        Seq(
          MemoryLayout.paddingLayout((alignment - misalignment).toBits).nn
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
    case PtrDescriptor    => Bytes(toMemoryLayout(PtrDescriptor).byteSize())
    case sd: StructDescriptor =>
      Bytes(toGroupLayout(sd).byteSize())
    case VaListDescriptor => Bytes(toMemoryLayout(VaListDescriptor).byteSize())
    case ad: AliasDescriptor[?]          => sizeOf(ad.real)
    case CUnionDescriptor(possibleTypes) => possibleTypes.map(sizeOf).max

  override def alignmentOf(td: TypeDescriptor): Bytes = td match
    case s: StructDescriptor =>
      s.members.view.map(_.descriptor).map(alignmentOf).max
    case CUnionDescriptor(possibleTypes) =>
      possibleTypes.view.map(alignmentOf).max
    case _ => sizeOf(td)

  override def memberOffsets(sd: List[TypeDescriptor]): IArray[Bytes] =
    offsets.getOrElseUpdate(
      sd, {
        val ll = genLayoutList(
          sd.map(toMemoryLayout(_).withName("").nn),
          sd.view.map(alignmentOf).max
        )
        IArray.from(
          ll match
            case head :: next =>
              next
                .foldLeft(Seq(Bytes(0)) -> head.byteSize()) {
                  case ((offsets, lastSize), layout) =>
                    val newSize = lastSize + layout.byteSize()
                    val newOffsets =
                      if layout.name.nn.isPresent() then
                        offsets :+ Bytes(lastSize)
                      else offsets
                    (newOffsets, newSize)
                }
                ._1
            case _ =>
              Seq.empty
        )
      }
    )

  def toMemoryLayout(td: TypeDescriptor): MemoryLayout = td match
    case ByteDescriptor         => C_CHAR.nn
    case ShortDescriptor        => C_SHORT.nn
    case IntDescriptor          => C_INT.nn
    case LongDescriptor         => C_LONG_LONG.nn
    case FloatDescriptor        => C_FLOAT.nn
    case DoubleDescriptor       => C_DOUBLE.nn
    case PtrDescriptor          => C_POINTER.nn
    case VaListDescriptor       => C_POINTER.nn
    case sd: StructDescriptor   => toGroupLayout(sd)
    case ad: AliasDescriptor[?] => toMemoryLayout(ad.real)
    case CUnionDescriptor(possibleTypes) =>
      MemoryLayout.unionLayout(possibleTypes.map(toMemoryLayout).toSeq*).nn

  def toMemoryLayout(smd: StructMemberDescriptor): MemoryLayout =
    toMemoryLayout(smd.descriptor).withName(smd.name).nn

  def toGroupLayout(sd: StructDescriptor): GroupLayout =
    chm.getOrElseUpdate(
      sd, {
        val originalMembers = sd.members.map(toMemoryLayout)
        val alignment = alignmentOf(sd)
        MemoryLayout
          .structLayout(genLayoutList(originalMembers, alignment)*)
          .nn
          .withName(sd.clazz.getName())
          .nn
      }
    )

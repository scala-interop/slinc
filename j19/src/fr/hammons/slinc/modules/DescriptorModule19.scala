package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.lang.foreign.ValueLayout
import scala.collection.concurrent.TrieMap
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryAddress
import java.lang.foreign.MemorySegment

given DescriptorModule with
  private val sdt = TrieMap.empty[StructDescriptor, StructLayout]
  private val offsets = TrieMap.empty[StructDescriptor, IArray[Bytes]]
  
  override def toDataLayout(td: TypeDescriptor): DataLayout = td match
    case ByteDescriptor       => LayoutI19.byteLayout
    case ShortDescriptor      => LayoutI19.shortLayout
    case IntDescriptor        => LayoutI19.intLayout
    case LongDescriptor       => LayoutI19.longLayout
    case FloatDescriptor      => LayoutI19.floatLayout
    case DoubleDescriptor     => LayoutI19.doubleLayout
    case PtrDescriptor        => LayoutI19.pointerLayout
    case sd: StructDescriptor => toStructLayout(sd)


  def toCarrierType(td: TypeDescriptor): Class[?] = td match
    case ByteDescriptor => classOf[Byte]
    case ShortDescriptor => classOf[Short]
    case IntDescriptor => classOf[Int]
    case LongDescriptor => classOf[Long]
    case FloatDescriptor => classOf[Float]
    case DoubleDescriptor => classOf[Double]
    case PtrDescriptor => classOf[MemoryAddress]
    case _: StructDescriptor => classOf[MemorySegment]
  

  def toDataLayout(smd: StructMemberDescriptor): DataLayout =
    toDataLayout(smd.descriptor).withName(smd.name)

  override def memberOffsets(sd: StructDescriptor): IArray[Bytes] =
    offsets.getOrElseUpdate(
      sd, {
        val ll = genDataLayoutList(
          sd.members.view.map(toDataLayout).toSeq,
          sd.members.view.map(_.descriptor).map(alignmentOf).map(_.toLong).max
        )
        IArray.from(
          ll match
            case head :: next =>
              next
                .foldLeft((Seq(Bytes(0)), head.size)) {
                  case ((offsets, lastSize), layout) =>
                    val newSize = lastSize + layout.size
                    val newOffsets =
                      if layout.name.isDefined then offsets :+ lastSize
                      else offsets
                    (newOffsets, newSize)
                }
                ._1
            case _ =>
              Seq.empty
        )
      }
    )

  override def sizeOf(td: TypeDescriptor): Bytes = td match
    case ByteDescriptor       => Bytes(1)
    case ShortDescriptor      => Bytes(2)
    case IntDescriptor        => Bytes(4)
    case LongDescriptor       => Bytes(8)
    case FloatDescriptor      => Bytes(4)
    case DoubleDescriptor     => Bytes(8)
    case PtrDescriptor        => Bytes(ValueLayout.ADDRESS.nn.byteSize())
    case sd: StructDescriptor => toStructLayout(sd).size

  override def toStructLayout(sd: StructDescriptor): StructLayout =
    sdt.getOrElseUpdate(
      sd, {
        val originalMembers = sd.members.map(toDataLayout)
        val alignment = alignmentOf(sd)
        val offsets = memberOffsets(sd)
        val structMembers =
          genDataLayoutList(originalMembers, alignment.toLong)
            .foldLeft(Vector.empty[StructMember] -> 0) {
              case ((vector, offsetsIndex), dataLayout) =>
                if dataLayout.name.isDefined then
                  (
                    vector :+ StructMember(
                      dataLayout,
                      dataLayout.name,
                      offsets(offsetsIndex)
                    ),
                    offsetsIndex + 1
                  )
                else
                  (
                    vector :+ StructMember(dataLayout, None, Bytes(0)),
                    offsetsIndex
                  )
            }
            ._1

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
      }
    )

  def genDataLayoutList(
      layouts: Seq[DataLayout],
      alignment: Long
  ): Seq[DataLayout] =
    val (vector, currentLocation) = layouts
      .foldLeft(Seq.empty[DataLayout] -> 0L) {
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

  override def alignmentOf(td: TypeDescriptor): Bytes =
    import java.lang.foreign.ValueLayout
    td match
      case ByteDescriptor   => Bytes(1)
      case ShortDescriptor  => Bytes(2)
      case IntDescriptor    => Bytes(4)
      case LongDescriptor   => Bytes(8)
      case FloatDescriptor  => Bytes(4)
      case DoubleDescriptor => Bytes(8)
      case PtrDescriptor    => Bytes(ValueLayout.ADDRESS.nn.byteAlignment())
      case sd: StructDescriptor =>
        Bytes(
          sd.members.view.map(_.descriptor).map(alignmentOf).map(_.toLong).max
        )

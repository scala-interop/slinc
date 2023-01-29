package fr.hammons.slinc.modules

import fr.hammons.slinc.*
import java.lang.foreign.ValueLayout
import scala.collection.concurrent.TrieMap
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryAddress
import java.lang.foreign.MemorySegment
import java.lang.foreign.GroupLayout

given descriptorModule19: DescriptorModule with
  private val sdt = TrieMap.empty[StructDescriptor, GroupLayout]
  private val offsets = TrieMap.empty[StructDescriptor, IArray[Bytes]]

  def toMemoryLayout(td: TypeDescriptor): MemoryLayout = td match
    case ByteDescriptor       => ValueLayout.JAVA_BYTE.nn
    case ShortDescriptor      => ValueLayout.JAVA_SHORT.nn
    case IntDescriptor        => ValueLayout.JAVA_INT.nn
    case LongDescriptor       => ValueLayout.JAVA_LONG.nn
    case FloatDescriptor      => ValueLayout.JAVA_FLOAT.nn
    case DoubleDescriptor     => ValueLayout.JAVA_DOUBLE.nn
    case PtrDescriptor        => ValueLayout.ADDRESS.nn
    case sd: StructDescriptor => toGroupLayout(sd)

  def toMemoryLayout(smd: StructMemberDescriptor): MemoryLayout =
    toMemoryLayout(smd.descriptor).withName(smd.name).nn

  def toGroupLayout(sd: StructDescriptor): GroupLayout =
    sdt.getOrElseUpdate(
      sd, {
        MemoryLayout
          .structLayout(
            genLayoutList(
              sd.members.map(toMemoryLayout),
              alignmentOf(sd).toLong
            )*
          )
          .nn
      }
    )

  def genLayoutList(
      layouts: Seq[MemoryLayout],
      alignment: Long
  ): Seq[MemoryLayout] =
    val (vector, currentLocation) =
      layouts.foldLeft(Seq.empty[MemoryLayout] -> 0L) {
        case ((vector, currentLocation), layout) =>
          val thisAlignment = layout.byteAlignment()
          val misalignment = currentLocation % thisAlignment
          val toAdd =
            if misalignment == 0 then Seq(layout)
            else
              val paddingNeeded = thisAlignment - misalignment

              Seq(
                MemoryLayout.paddingLayout(paddingNeeded * 8).nn,
                layout
              )
          (vector ++ toAdd, currentLocation + toAdd.view.map(_.byteSize()).sum)
      }
    val misalignment = currentLocation % alignment
    vector ++ (
      if misalignment != 0 then
        Seq(
          MemoryLayout.paddingLayout((alignment - misalignment) * 8).nn
        )
      else Seq.empty
    )

  def toCarrierType(td: TypeDescriptor): Class[?] = td match
    case ByteDescriptor      => classOf[Byte]
    case ShortDescriptor     => classOf[Short]
    case IntDescriptor       => classOf[Int]
    case LongDescriptor      => classOf[Long]
    case FloatDescriptor     => classOf[Float]
    case DoubleDescriptor    => classOf[Double]
    case PtrDescriptor       => classOf[MemoryAddress]
    case _: StructDescriptor => classOf[MemorySegment]

  override def memberOffsets(sd: StructDescriptor): IArray[Bytes] =
    offsets.getOrElseUpdate(
      sd, {
        val ll = genLayoutList(
          sd.members.view.map(toMemoryLayout).toSeq,
          sd.members.view.map(_.descriptor).map(alignmentOf).map(_.toLong).max
        )
        IArray.from(
          ll match
            case head :: next =>
              next
                .foldLeft((Seq(Bytes(0)), head.byteSize())) {
                  case ((offsets, lastSize), layout) =>
                    val newSize = lastSize + layout.byteSize()
                    val newOffsets =
                      if layout.name().nn.isPresent() then
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

  override def sizeOf(td: TypeDescriptor): Bytes = td match
    case ByteDescriptor       => Bytes(1)
    case ShortDescriptor      => Bytes(2)
    case IntDescriptor        => Bytes(4)
    case LongDescriptor       => Bytes(8)
    case FloatDescriptor      => Bytes(4)
    case DoubleDescriptor     => Bytes(8)
    case PtrDescriptor        => Bytes(ValueLayout.ADDRESS.nn.byteSize())
    case sd: StructDescriptor => Bytes(toGroupLayout(sd).byteSize())

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

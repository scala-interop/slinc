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
  private val offsets = TrieMap.empty[List[TypeDescriptor], IArray[Bytes]]

  def toMemoryLayout(td: TypeDescriptor): MemoryLayout = td match
    case ByteDescriptor         => ValueLayout.JAVA_BYTE.nn
    case ShortDescriptor        => ValueLayout.JAVA_SHORT.nn
    case IntDescriptor          => ValueLayout.JAVA_INT.nn
    case LongDescriptor         => ValueLayout.JAVA_LONG.nn
    case FloatDescriptor        => ValueLayout.JAVA_FLOAT.nn
    case DoubleDescriptor       => ValueLayout.JAVA_DOUBLE.nn
    case PtrDescriptor          => ValueLayout.ADDRESS.nn
    case VaListDescriptor       => ValueLayout.ADDRESS.nn
    case sd: StructDescriptor   => toGroupLayout(sd)
    case ad: AliasDescriptor[?] => toMemoryLayout(ad.real)
    case CUnionDescriptor(possibleTypes) =>
      MemoryLayout.unionLayout(possibleTypes.view.map(toMemoryLayout).toSeq*).nn

  def toMemoryLayout(smd: StructMemberDescriptor): MemoryLayout =
    toMemoryLayout(smd.descriptor).withName(smd.name).nn

  def toGroupLayout(sd: StructDescriptor): GroupLayout =
    sdt.getOrElseUpdate(
      sd, {
        MemoryLayout
          .structLayout(
            genLayoutList(
              sd.members.map(toMemoryLayout),
              Bytes(
                sd.members.view.map(toMemoryLayout).map(_.byteAlignment()).max
              )
            )*
          )
          .nn
      }
    )

  def genLayoutList(
      layouts: Seq[MemoryLayout],
      alignment: Bytes
  ): Seq[MemoryLayout] =
    val (vector, currentLocation) =
      layouts.foldLeft(Seq.empty[MemoryLayout] -> Bytes(0)) {
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

  def toCarrierType(td: TypeDescriptor): Class[?] = td match
    case ByteDescriptor         => classOf[Byte]
    case ShortDescriptor        => classOf[Short]
    case IntDescriptor          => classOf[Int]
    case LongDescriptor         => classOf[Long]
    case FloatDescriptor        => classOf[Float]
    case DoubleDescriptor       => classOf[Double]
    case VaListDescriptor       => classOf[MemoryAddress]
    case PtrDescriptor          => classOf[MemoryAddress]
    case _: StructDescriptor    => classOf[MemorySegment]
    case ad: AliasDescriptor[?] => toCarrierType(ad.real)
    case CUnionDescriptor(_)    => classOf[MemorySegment]

  override def memberOffsets(sd: List[TypeDescriptor]): IArray[Bytes] =
    offsets.getOrElseUpdate(
      sd, {
        val ll = genLayoutList(
          sd.map(toMemoryLayout(_).withName("").nn).toSeq,
          sd.map(alignmentOf).max
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

  override def sizeOf(td: TypeDescriptor): Bytes = Bytes(
    toMemoryLayout(td).byteSize()
  )

  override def alignmentOf(td: TypeDescriptor): Bytes = Bytes(
    toMemoryLayout(td).byteAlignment()
  )

package fr.hammons.slinc.modules

import fr.hammons.slinc.*

import jdk.incubator.foreign.{
  MemoryLayout,
  MemoryAddress,
  MemorySegment,
  GroupLayout,
  CLinker,
  ValueLayout
}, CLinker.C_POINTER
import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.types.{arch, os, OS, Arch}
import fr.hammons.slinc.modules.platform.*

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

  override def sizeOf(td: TypeDescriptor): Bytes = Bytes(
    toMemoryLayout(td).byteSize()
  )

  override def alignmentOf(td: TypeDescriptor): Bytes = Bytes(
    toMemoryLayout(td).byteAlignment
  )

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

  val platform = (os, arch) match
    case (OS.Linux, Arch.X64)      => x64.Linux
    case (OS.Darwin, Arch.X64)     => x64.Darwin
    case (OS.Windows, Arch.X64)    => x64.Windows
    case (OS.Linux, Arch.AArch64)  => aarch64.Linux
    case (OS.Darwin, Arch.AArch64) => aarch64.Darwin
    case _                         => throw Error("Unsupported platform!")

  def toMemoryLayout(td: TypeDescriptor): MemoryLayout = td match
    case ByteDescriptor         => platform.jByte
    case ShortDescriptor        => platform.jShort
    case IntDescriptor          => platform.jInt
    case LongDescriptor         => platform.jLong
    case FloatDescriptor        => platform.jFloat
    case DoubleDescriptor       => platform.jDouble
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
        val alignment = Bytes(originalMembers.view.map(_.byteAlignment()).max)
        MemoryLayout
          .structLayout(genLayoutList(originalMembers, alignment)*)
          .nn
          .withName(sd.clazz.getName())
          .nn
      }
    )

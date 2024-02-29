package fr.hammons.slinc.descriptors

import fr.hammons.slinc.modules.ReadWriteModule
import fr.hammons.slinc.modules.DescriptorModule
import fr.hammons.slinc.modules.Reader
import fr.hammons.slinc.modules.readWriteModule
import fr.hammons.slinc.modules.Writer

sealed trait ForeignDescriptor[A] {
  val reader: (ReadWriteModule, DescriptorModule) ?=> Reader[A]
  val writer: (ReadWriteModule, DescriptorModule) ?=> Writer[A]

}

object ForeignDescriptor:
  given byteDescriptor: ForeignDescriptor[Byte] with
    val reader = readWriteModule.byteReader
    val writer = readWriteModule.byteWriter

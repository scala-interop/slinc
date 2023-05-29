package fr.hammons.slinc.descriptors

import fr.hammons.slinc.modules.DescriptorModule
import fr.hammons.slinc.modules.ReadWriteModule

final case class WriterContext(dm: DescriptorModule, rwm: ReadWriteModule)

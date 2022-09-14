package fr.hammons.sffi

import jdk.incubator.foreign.MemoryLayout, MemoryLayout.PathElement
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.CLinker.*
import scala.jdk.CollectionConverters.*

object Layout17 extends Layout.PlatformSpecific[MemoryLayout]:

  override def withName(context: MemoryLayout, name: String): MemoryLayout =
    context.withName(name).nn

  override val floatLayout: MemoryLayout = C_FLOAT.nn

  override val byteLayout: MemoryLayout = C_CHAR.nn

  override val intLayout: MemoryLayout = C_INT.nn

  override val longLayout: MemoryLayout = C_LONG.nn

  override def groupLayout(layouts: List[MemoryLayout]): MemoryLayout =
    MemoryLayout.structLayout(layouts*).nn

  override def offsetsOf(context: MemoryLayout): IArray[Long] = context match 
    case g: GroupLayout => 
      val paths = g
      .memberLayouts()
      .nn
      .asScala
      .map(_.name().nn.get.nn)
      .map(PathElement.groupElement(_).nn)
    

      IArray.from(paths.map(g.byteOffset(_)))

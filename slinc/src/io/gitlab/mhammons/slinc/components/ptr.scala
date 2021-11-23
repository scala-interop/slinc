package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryAccess}

/** The native version of Ptr * */
class ptr[A](memorySegment: MemorySegment, offset: Long, layout: MemLayout):
   val properTemplate = layout.template.andThen(_.asInstanceOf[A])
   def apply(): Ptr[A] = Ptr(
     MemoryAccess.getAddressAtOffset(memorySegment, offset),
     layout.byteSize(),
     properTemplate
   )
   def update(a: Ptr[A]): Unit =
      MemoryAccess.setAddressAtOffset(memorySegment, offset, a.memoryAddress)

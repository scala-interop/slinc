package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryAccess}

/** The native version of Ptr * */
class ptr[A: Template](
    memorySegment: MemorySegment,
    offset: Long
):
   def apply(): Ptr[A] = Ptr(
     MemoryAccess.getAddressAtOffset(memorySegment, offset)
   )
   def update(a: Ptr[A]): Unit =
      MemoryAccess.setAddressAtOffset(memorySegment, offset, a.memoryAddress)

object ptr:
   given [A](using Template[A]): Template[ptr[A]] =
      PtrTemplate(summon[Template[A]].layout, Nil)

package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryLayout},
MemoryLayout.PathElement

class PtrTemplate[A: Template](val layout: MemoryLayout, val path: Seq[PathElement])
    extends Template[ptr[A]]:

   def apply(memorySegment: MemorySegment) =
      ptr(memorySegment, layout.byteOffset(path*))

   def subTemplate(memoryLayout: MemoryLayout, path: Seq[PathElement]) =
      PtrTemplate(memoryLayout, path)

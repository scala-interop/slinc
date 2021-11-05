package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.CLinker.*

trait HasMemoryLayout[T]:
   val layout: MemoryLayout

object HasMemoryLayout:
   given HasMemoryLayout[Int] with
      val layout = C_INT

   given HasMemoryLayout[Float] with
      val layout = C_FLOAT

   given [T]: HasMemoryLayout[Ptr[T]] with
      val layout = C_POINTER

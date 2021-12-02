package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemoryLayout, CLinker}, CLinker.{C_INT}

trait NativeInfo[T]:
  val memoryLayout: MemoryLayout
  val carrierType: Class[?]

object NativeInfo:
   given NativeInfo[Int] with
      val memoryLayout = C_INT
      val carrierType = classOf[Int]

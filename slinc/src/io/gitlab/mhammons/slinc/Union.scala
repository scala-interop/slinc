package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemorySegment

trait Union(
    map: Map[String, MemorySegment => Any],
    memorySegment: MemorySegment
):
   def selectDynamic(key: String) = map(key)(memorySegment)

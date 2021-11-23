package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.MemorySegment

class MapStruct(
    map: Map[String, Any],
    memorySegment: MemorySegment,
    layout: MemLayout
)

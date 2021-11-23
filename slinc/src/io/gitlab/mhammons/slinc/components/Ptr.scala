package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemoryAddress, MemoryAccess, MemorySegment}
import java.lang.annotation.Native
import scala.compiletime.{erasedValue, summonInline}
import io.gitlab.mhammons.slinc.NativeCache
import io.gitlab.mhammons.slinc.StructMacros
import io.gitlab.mhammons.slinc.Struct

class Ptr[T](
    private[components] val memoryAddress: MemoryAddress,
    segmentSize: Long,
    template: MemorySegment => T
):
   def `unary_!` : T = template(
     memoryAddress.asSegment(segmentSize, memoryAddress.scope)
   )

package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.MemoryAddress
import java.lang.annotation.Native
import scala.compiletime.{erasedValue, summonInline}
import io.gitlab.mhammons.slinc.NativeCache

trait Ptr[T](memoryAddress: MemoryAddress):
   def mem = memoryAddress
   def `unary_!` : T

object Ptr:
   inline def apply[T](t: T) =
      inline t match
         case s: Struct =>
            val size = summonInline[NativeCache].layout2[T].underlying.byteSize
            new Ptr[T](s.$addr) {
               def `unary_!` = Segment(
                 Map.empty, // todo: must be replaced
                 s.$addr.asSegment(size, s.$addr.scope())
               ).asInstanceOf[T]
            }

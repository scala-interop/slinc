package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryAddress}

trait Struct(vals: Map[String, Any]) extends Selectable:
   def selectDynamic(key: String) = vals(key)
   private[components] def $addr: MemoryAddress

object Struct:
   extension [S <: Struct](s: S) inline def `unary_~` : Ptr[S] = Ptr[S](s)

class Segment(vals: Map[String, Any], mem: MemorySegment) extends Struct(vals):
   private[components] def $addr = mem.address

class Subsegment(vals: Map[String, Any], root: MemorySegment, offset: Long)
    extends Struct(vals):
   private[components] def $addr = root.address.addOffset(offset)

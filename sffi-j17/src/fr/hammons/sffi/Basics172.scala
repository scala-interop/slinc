package fr.hammons.sffi

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.MemorySegment 
object Basics172 extends BasicsI:
  type Context = MemoryLayout
  type RawMem = MemorySegment
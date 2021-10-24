package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemoryAddress

opaque type Ptr[A] = MemoryAddress

object Ptr:
  def apply[A](mem: MemoryAddress): Ptr[A] = mem

extension [A](p: Ptr[A]) def getAddr: MemoryAddress = p

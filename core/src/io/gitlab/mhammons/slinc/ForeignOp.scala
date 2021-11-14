package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemoryLayout

enum ForeignOp[A]:
   case Pure[A](a: A) extends ForeignOp[A]
   case CachedLayout(name: String) extends ForeignOp[Option[MemoryLayout]]

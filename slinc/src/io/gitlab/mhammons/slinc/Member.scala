package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemorySegment
import java.lang.invoke.VarHandle
import io.gitlab.mhammons.polymorphics.VarHandleHandler

class Member[T](memSgmnt: MemorySegment, varHandle: VarHandle):
   def update(t: T) = VarHandleHandler.set(varHandle, memSgmnt, t)
   def apply(): T = VarHandleHandler.get(varHandle, memSgmnt).asInstanceOf[T]

   private[slinc] val mem = memSgmnt

object Member:
   type int = Member[Int]
   type float = Member[Float]
   type long = Member[Long]

package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemorySegment, MemoryLayout},
MemoryLayout.PathElement
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import java.lang.invoke.VarHandle
import jdk.incubator.foreign.CLinker.{C_INT, C_FLOAT}

class Member[T](
    memSgmnt: MemorySegment,
    varHandle: VarHandle,
    layout: MemoryLayout,
    path: Seq[PathElement]
)(using Template[Member[T]]):
   def update(t: T) = VarHandleHandler.set(varHandle, memSgmnt, t)
   def apply(): T = VarHandleHandler.get(varHandle, memSgmnt).asInstanceOf[T]
   def `unary_~` = Ptr[Member[T]](memSgmnt.address)

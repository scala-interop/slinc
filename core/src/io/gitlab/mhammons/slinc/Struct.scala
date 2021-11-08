package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemorySegment
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import jdk.incubator.foreign.GroupLayout
import jdk.incubator.foreign.MemoryLayout

class Fd[T](memSgmnt: MemorySegment, varHandle: VarHandleHandler)
    extends FieldLike[T]:
   def set(t: T) = varHandle.set(memSgmnt, t)
   def get: T = varHandle.get(memSgmnt).asInstanceOf[T]

   private[slinc] val mem = memSgmnt

class FdStruct[T <: Struct](
    memSgmnt: MemorySegment,
    varHandle: VarHandleHandler,
    str: T
) extends FieldLike[T]:
   def get: T = str
   def set(t: T): Unit = varHandle.set(memSgmnt, t.$mem)

object Fd:
   type int = Fd[Int]
   type float = Fd[Float]
   type long = Fd[Long]

class Struct(vals: Map[String, Any]) extends Selectable:
   def selectDynamic(name: String) = vals(name)

   val $mem: MemorySegment = vals("$mem").asInstanceOf[MemorySegment]

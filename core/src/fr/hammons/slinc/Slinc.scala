package fr.hammons.slinc

import scala.concurrent.ExecutionContext
import scala.quoted.staging.Compiler
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import dotty.tools.dotc.config.Platform
import java.util.concurrent.ThreadFactory

trait Slinc(
    layoutPlatformSpecific: LayoutI.PlatformSpecific,
    allocatorPlatformSpecific: Allocator.PlatformSpecific,
    jitManager: JitManager
):
  private val useJit = Option(System.getProperty("sffi-jit"))
    .flatMap(_.nn.toBooleanOption)
    .getOrElse(true)
  protected val layoutI = LayoutI(layoutPlatformSpecific)
  protected val structI = StructI(layoutI, jitManager)
  protected val typesI = TypesI.platformTypes(layoutI)
  export layoutI.{*, given}
  export typesI.{*, given}
  export structI.Struct

  extension (l: Long) def toBytes = Bytes(l)

  object Allocator:
    def global[A](fn: Allocator ?=> A) = 
      given alloc: Allocator = allocatorPlatformSpecific.globalAllocator()
      val result = fn
      alloc.close()
      result

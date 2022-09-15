package fr.hammons.sffi

import scala.concurrent.ExecutionContext
import scala.quoted.staging.Compiler
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import dotty.tools.dotc.config.Platform
import java.util.concurrent.ThreadFactory


trait FFI3(layoutPlatformSpecific: LayoutI.PlatformSpecific, allocatorPlatformSpecific: Allocator.PlatformSpecific):
  protected val layoutI = LayoutI(layoutPlatformSpecific)
  protected given comp: Compiler
  private val tp = Executors.newWorkStealingPool(1).nn
  private val runnable: Runnable = () => tp.shutdown()
  Runtime.getRuntime().nn.addShutdownHook(Thread(runnable))
  protected val compExecutor = ExecutionContext.fromExecutor(tp.nn)
  protected val structI = StructI(layoutI, compExecutor)
  export layoutI.{*,given}
  export structI.Struct

  extension (l: Long) def toBytes = Bytes(l)
  

  object Allocator:
    export allocatorPlatformSpecific.globalAllocator
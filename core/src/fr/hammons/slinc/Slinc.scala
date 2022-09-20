package fr.hammons.slinc

import scala.concurrent.ExecutionContext
import scala.quoted.staging.Compiler
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import dotty.tools.dotc.config.Platform
import java.util.concurrent.ThreadFactory

trait Slinc(
    layoutPlatformSpecific: LayoutI.PlatformSpecific,
    allocatorPlatformSpecific: Allocator.PlatformSpecific
):
  private val useJit = Option(System.getProperty("sffi-jit"))
    .flatMap(_.nn.toBooleanOption)
    .getOrElse(true)
  protected val layoutI = LayoutI(layoutPlatformSpecific)
  protected def comp: Compiler
  protected val jitService =
    if useJit then JitManagerImpl(comp) else DummyManager
  protected val structI = StructI(layoutI, jitService)
  protected val typesI = TypesI.platformTypes(layoutI)
  export layoutI.{*, given}
  export typesI.{*, given}
  export structI.Struct

  private[slinc] def forceJit() = jitService.jitNow()

  extension (l: Long) def toBytes = Bytes(l)

  object Allocator:
    export allocatorPlatformSpecific.globalAllocator

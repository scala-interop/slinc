package fr.hammons.slinc

import java.lang.foreign.Linker
import fr.hammons.slinc.modules.{*, given}

class Slinc19(_jitManager: JitManager, linker: Linker)(using
    val dm: DescriptorModule,
    val tm: TransitionModule,
    val rwm: ReadWriteModule,
    val lm: FSetModule
) extends Slinc:
  val version: Int = 19
  protected def jitManager: JitManager = _jitManager

  protected def scopePlatformSpecific: ScopeI.PlatformSpecific =
    Scope19(linker)

@SlincImpl(19)
object Slinc19:
  private val compiler =
    scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private[slinc] val linker = Linker.nativeLinker().nn
  val default = Slinc19(JitManagerImpl(compiler), linker)
  val noJit = Slinc19(NoJitManager, linker)
  val immediateJit = Slinc19(InstantJitManager(compiler), linker)

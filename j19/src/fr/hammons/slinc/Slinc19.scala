package fr.hammons.slinc

import fr.hammons.slinc.LayoutI.PlatformSpecific
import java.lang.foreign.Linker

class Slinc19(_jitManager: JitManager, linker: Linker) extends Slinc:
  protected def jitManager: JitManager = _jitManager

  protected def layoutPlatformSpecific = LayoutI19

  protected def scopePlatformSpecific: ScopeI.PlatformSpecific =
    Scope19(layoutI, linker)

  protected def transitionsPlatformSpecific: TransitionsI.PlatformSpecific =
    Transitions19

  protected def libraryIPlatformSpecific: LibraryI.PlatformSpecific =
    Library19(layoutI, linker)

@SlincImpl(19)
object Slinc19:
  private val compiler =
    scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private val linker = Linker.nativeLinker().nn
  val default = Slinc19(JitManagerImpl(compiler), linker)
  val noJit = Slinc19(NoJitManager, linker)
  val immediateJit = Slinc19(InstantJitManager(compiler), linker)

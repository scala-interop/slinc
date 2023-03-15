package fr.hammons.slinc

import jdk.incubator.foreign.CLinker
import fr.hammons.slinc.modules.{
  DescriptorModule,
  TransitionModule,
  ReadWriteModule,
  LibModule
}
import fr.hammons.slinc.modules.given

class Slinc17(_jitManager: JitManager, linker: CLinker)(using
    val dm: DescriptorModule,
    val tm: TransitionModule,
    val rwm: ReadWriteModule,
    val lm: LibModule
) extends Slinc:
  protected def jitManager = _jitManager
  protected def scopePlatformSpecific = Scope17(linker)
  protected def libraryIPlatformSpecific = Library17(linker)

@SlincImpl(17)
object Slinc17:
  private val compiler =
    scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private[slinc] val linker = CLinker.getInstance().nn
  val default = Slinc17(JitManagerImpl(compiler), linker)
  val noJit = Slinc17(NoJitManager, linker)
  val immediateJit = Slinc17(InstantJitManager(compiler), linker)

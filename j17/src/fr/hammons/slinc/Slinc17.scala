package fr.hammons.slinc

import jdk.incubator.foreign.CLinker
import fr.hammons.slinc.modules.{
  DescriptorModule,
  TransitionModule,
  ReadWriteModule,
  FSetModule
}
import fr.hammons.slinc.modules.given

class Slinc17(_jitManager: JitManager)(using
    val dm: DescriptorModule,
    val tm: TransitionModule,
    val rwm: ReadWriteModule,
    val lm: FSetModule
) extends Slinc:
  val version: Int = 17
  protected def jitManager = _jitManager
  protected def scopePlatformSpecific = Scope17

@SlincImpl(17)
object Slinc17:
  private lazy val compiler =
    scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private[slinc] lazy val linker = CLinker.getInstance().nn
  val default = Slinc17(JitManagerImpl(compiler))
  val noJit = Slinc17(NoJitManager)
  val immediateJit = Slinc17(InstantJitManager(compiler))

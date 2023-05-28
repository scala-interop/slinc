package fr.hammons.slinc

import jdk.incubator.foreign.CLinker
import fr.hammons.slinc.modules.{
  DescriptorModule,
  TransitionModule,
  ReadWriteModule,
  FSetModule
}
import fr.hammons.slinc.modules.given

class Slinc17(using
    val dm: DescriptorModule,
    val tm: TransitionModule,
    val rwm: ReadWriteModule,
    val lm: FSetModule
) extends Slinc:
  val version: Int = 17
  protected def scopePlatformSpecific = Scope17

@SlincImpl(17)
object Slinc17:
  private lazy val compiler =
    scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private[slinc] lazy val linker = CLinker.getInstance().nn
  val default = Slinc17()
  val noJit = Slinc17()
  val immediateJit = Slinc17()

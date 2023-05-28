package fr.hammons.slinc

import java.lang.foreign.Linker
import fr.hammons.slinc.modules.{*, given}

class Slinc19()(using
    val dm: DescriptorModule,
    val tm: TransitionModule,
    val rwm: ReadWriteModule,
    val lm: FSetModule
) extends Slinc:
  val version: Int = 19

  protected def scopePlatformSpecific: ScopeI.PlatformSpecific =
    Scope19

@SlincImpl(19)
object Slinc19:
  private val compiler =
    scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private[slinc] lazy val linker = Linker.nativeLinker().nn
  val default = Slinc19()
  val noJit = Slinc19()
  val immediateJit = Slinc19()

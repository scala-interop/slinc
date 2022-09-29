package fr.hammons.slinc

class Slinc19(jitManager: JitManager)
    extends Slinc(LayoutI19, Scope19(_), Transitions19, Library19(_), jitManager)

object Slinc19:
  private val compiler = scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  val default = Slinc19(JitManagerImpl(compiler))
  val noJit = Slinc19(NoJitManager)
  val immediateJit = Slinc19(InstantJitManager(compiler))

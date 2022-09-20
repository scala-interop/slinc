package fr.hammons.slinc

class Slinc17(jitManager: JitManager) extends Slinc(LayoutI17,Allocator17, jitManager)

object Slinc17:
  private val compiler = scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  val default = Slinc17(JitManagerImpl(compiler))
  val noJit = Slinc17(NoJitManager)
  val immediateJit = Slinc17(InstantJitManager(compiler))
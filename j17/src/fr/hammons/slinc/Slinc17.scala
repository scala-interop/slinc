package fr.hammons.slinc

import jdk.incubator.foreign.CLinker

class Slinc17(jitManager: JitManager, linker: CLinker) extends Slinc(LayoutI17,Scope17(_, linker), Transitions17, Library17(_, linker), jitManager)

object Slinc17:
  private val compiler = scala.quoted.staging.Compiler.make(getClass().getClassLoader().nn)
  private val linker = CLinker.getInstance().nn
  val default = Slinc17(JitManagerImpl(compiler), linker)
  val noJit = Slinc17(NoJitManager, linker)
  val immediateJit = Slinc17(InstantJitManager(compiler), linker)
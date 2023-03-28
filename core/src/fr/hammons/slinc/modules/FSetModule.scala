package fr.hammons.slinc.modules

import fr.hammons.slinc.FSetBacking
import fr.hammons.slinc.FunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor

trait FSetModule:
  val runtimeVersion: Int
  def getBacking(
      desc: List[CFunctionDescriptor],
      generators: List[FunctionBindingGenerator]
  ): FSetBacking[?]

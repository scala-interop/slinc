package fr.hammons.slinc.modules

import fr.hammons.slinc.FSetBacking
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor

trait FSetModule:
  val runtimeVersion: Int
  def getBacking(
      desc: List[CFunctionDescriptor],
      generators: List[CFunctionBindingGenerator]
  ): FSetBacking[?]

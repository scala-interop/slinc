package fr.hammons.slinc.modules

import fr.hammons.slinc.fset.FSetBacking
import fr.hammons.slinc.FunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor
import fr.hammons.slinc.fset.Dependency

trait FSetModule:
  val runtimeVersion: Int
  def getBacking(
      dependencies: List[Dependency],
      desc: List[CFunctionDescriptor],
      generators: List[FunctionBindingGenerator]
  ): FSetBacking[?]

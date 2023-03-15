package fr.hammons.slinc.modules

import fr.hammons.slinc.LibBacking
import fr.hammons.slinc.FunctionDescriptor
import fr.hammons.slinc.CFunctionBindingGenerator

trait LibModule:
  val runtimeVersion: Int
  def getLibrary(
      desc: List[(String, FunctionDescriptor)],
      generators: List[CFunctionBindingGenerator]
  ): LibBacking[?]

package fr.hammons.slinc.modules

import fr.hammons.slinc.LibBacking
import fr.hammons.slinc.CFunctionBindingGenerator
import fr.hammons.slinc.CFunctionDescriptor

trait LibModule:
  val runtimeVersion: Int
  def getLibrary(
      desc: List[CFunctionDescriptor],
      generators: List[CFunctionBindingGenerator]
  ): LibBacking[?]

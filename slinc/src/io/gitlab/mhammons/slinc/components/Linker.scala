package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.CLinker

object Linker:
  val linker = CLinker.getInstance


package fr.hammons.slinc

val slinc = Slinc.getRuntime()

import slinc.{*, given}

class Bindings extends StdlibSpec(slinc)

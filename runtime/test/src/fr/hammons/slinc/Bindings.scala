package fr.hammons.slinc

val slinc = Slinc.getRuntime()

import slinc.{*, given}
object Mytest derives Library:
  def abs(i: Int): Int = Library.binding

val x = println(Mytest.abs(3))

class Bindings extends StdlibSpec(slinc)

package fr.hammons.sffi

import scala.quoted.staging.Compiler


object FFI173 extends FFI3(LayoutI17, Allocator17):
  protected def comp: Compiler = Compiler.make(getClass().getClassLoader().nn)
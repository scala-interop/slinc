package fr.hammons.slinc

import java.lang.invoke.VarHandle

trait Struct extends Selectable:
  val fieldHandles: Array[Field[?]]
  def selector(value: String): Int

  def selectDynamic(key: String): Any = ???

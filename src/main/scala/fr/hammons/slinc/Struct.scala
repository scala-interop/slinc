package fr.hammons.slinc

trait Struct extends Selectable:
  def selectDynamic(key: String): Any
  def applyDynamic(key: String)(args: Any*): Any

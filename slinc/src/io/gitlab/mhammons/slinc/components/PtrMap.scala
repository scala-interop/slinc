package io.gitlab.mhammons.slinc.components

class PtrMap(map: Map[String,Any]) extends Selectable:
   def selectDynamic(key: String):Any = map(key)
package fr.hammons.slinc.modules.platform.x64

import fr.hammons.slinc.modules.platform.Platform
import jdk.incubator.foreign.CLinker.{
  C_CHAR,
  C_DOUBLE,
  C_FLOAT,
  C_INT,
  C_LONG,
  C_SHORT
}

object Darwin extends Platform:
  val jByte = C_CHAR.nn
  val jShort = C_SHORT.nn
  val jInt = C_INT.nn
  val jLong = C_LONG.nn
  val jFloat = C_FLOAT.nn
  val jDouble = C_DOUBLE.nn

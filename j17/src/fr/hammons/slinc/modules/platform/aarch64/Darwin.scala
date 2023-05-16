package fr.hammons.slinc.modules.platform.aarch64

import fr.hammons.slinc.modules.platform.Platform
import jdk.incubator.foreign.CLinker.{
  C_CHAR,
  C_DOUBLE,
  C_FLOAT,
  C_INT,
  C_LONG,
  C_SHORT
}
import jdk.incubator.foreign.ValueLayout

object Darwin extends Platform:
  val jByte: ValueLayout = C_CHAR.nn
  val jShort: ValueLayout = C_SHORT.nn
  val jInt: ValueLayout = C_INT.nn
  val jLong: ValueLayout = C_LONG.nn
  val jFloat: ValueLayout = C_FLOAT.nn
  val jDouble: ValueLayout = C_DOUBLE.nn

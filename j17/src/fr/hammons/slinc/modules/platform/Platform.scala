package fr.hammons.slinc.modules.platform

import jdk.incubator.foreign.ValueLayout
trait Platform {
  val jByte: ValueLayout
  val jShort: ValueLayout
  val jInt: ValueLayout
  val jLong: ValueLayout
  val jFloat: ValueLayout
  val jDouble: ValueLayout
}

package fr.hammons.slinc.experimental

sealed trait Platform:
  val index: Int

sealed trait LinuxX64 extends Platform:
  val index: Int = 0
sealed trait MacX64 extends Platform:
  val index: Int = 1
sealed trait WinX64 extends Platform:
  val index: Int = 2

object Platform

package fr.hammons.slinc

import java.lang.invoke.VarHandle

trait Field[A](segment: Matchable):
  val varHandle: VarHandle
  def get: A = varHandle.get(segment)
  def set(a: A): Unit = varHandle.set(segment, a)

type >[A] = Field[A]

type IntF = >[Int]

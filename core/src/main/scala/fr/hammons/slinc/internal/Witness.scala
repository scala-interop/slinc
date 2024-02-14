package fr.hammons.slinc.internal

opaque type Witness[A] = Null

object Witness:
  def apply[A]: Witness[A] = null

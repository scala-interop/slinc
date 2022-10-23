package fr.hammons.slinc

trait InTransitionNeeded[A] extends NativeInCompatible[A]:
  def in(a: A): Object

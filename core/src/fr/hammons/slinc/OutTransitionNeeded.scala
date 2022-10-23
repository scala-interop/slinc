package fr.hammons.slinc

trait OutTransitionNeeded[A] extends NativeOutCompatible[A]:
  def out(obj: Object): A
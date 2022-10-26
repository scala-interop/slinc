package fr.hammons.slinc

trait InAllocatingTransitionNeeded[A] extends NativeInCompatible[A]:
  def in(a: A)(using Allocator): Object

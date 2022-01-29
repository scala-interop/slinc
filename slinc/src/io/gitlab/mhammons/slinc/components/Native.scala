package io.gitlab.mhammons.slinc.components

opaque type Native[A] <: A = A

object Native:
   def apply[A](a: A): Native[A] = a
package io.gitlab.mhammons.slinc

trait FieldLike[T]:
   def get: T
   def set(t: T): Unit

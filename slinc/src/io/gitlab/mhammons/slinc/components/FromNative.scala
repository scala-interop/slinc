package io.gitlab.mhammons.slinc.components

opaque type FromNative[A] = A

object FromNative:
   def apply[A](a: A): FromNative[A] = a
   given [A](using i: Immigrator[A]): Immigrator[FromNative[A]] = i
   given [A](using n: NativeInfo[A]): NativeInfo[FromNative[A]] = n
opaque type FromNativeVariadic[A] <: A = A

package fr.hammons.slinc

import java.lang.invoke.MethodHandle

final class MethodHandler(fn: Seq[Variadic] => MethodHandle):
  val nonVariadic: MethodHandle = fn(Nil)
  def variadic(types: Seq[Variadic]): MethodHandle = fn(types)

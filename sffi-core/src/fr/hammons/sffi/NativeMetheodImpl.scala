package fr.hammons.sffi

import java.lang.invoke.MethodHandle

class NativeMethod[Inputs <: Tuple, Output](private val under: MethodHandle)
object NativeMethod:
  def toNM[Inputs <: Tuple, Output](
      mh: MethodHandle
  ): NativeMethod[Inputs, Output] = NativeMethod(mh)
  def toMh[In <: Tuple,Out](nm: NativeMethod[In,Out]): MethodHandle = nm.under
opaque type VariadicNativeMethod[Inputs <: Tuple, Output] = String
object VariadicNativeMethod:
  def apply[Inputs <: Tuple, Output](
      fn: String
  ): VariadicNativeMethod[Inputs, Output] = fn

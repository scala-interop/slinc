package io.gitlab.mhammons.polymorphics

import java.lang.invoke.MethodHandle

object MethodHandleHandler {
   def call0(mh: MethodHandle) = mh.invoke()
   def call1(mh: MethodHandle, a: Any) = mh.invoke(a)
   def call2(mh: MethodHandle, a: Any, b: Any) = mh.invoke(a, b)
   def call3(mh: MethodHandle, a: Any, b: Any, c: Any) = mh.invoke(a, b, c)
}

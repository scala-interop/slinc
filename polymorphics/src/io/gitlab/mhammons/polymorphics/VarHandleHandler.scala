package io.gitlab.mhammons.polymorphics

import java.lang.invoke.VarHandle

object VarHandleHandler {
   def get[A](vh: VarHandle, a: A): Any = vh.get(a)
   def set[A, B](vh: VarHandle, a: A, b: B): Unit = vh.set(a, b)
}

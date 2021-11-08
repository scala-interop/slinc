package io.gitlab.mhammons.polymorphics

import java.lang.invoke.VarHandle

final case class VarHandleHandler(vh: VarHandle) extends AnyVal {
  def get[A](a: A) = vh.get(a)
  def set[A, B](a: A, v: B) = vh.set(a, v)
}

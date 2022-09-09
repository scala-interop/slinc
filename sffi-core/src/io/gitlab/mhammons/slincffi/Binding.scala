package io.gitlab.mhammons.slincffi

import java.lang.invoke.MethodHandle

trait Binding[Name, Shape]:
  def apply(): MethodHandle

package io.gitlab.mhammons.slinc.components

import java.lang.invoke.VarHandle

final case class NamedVarhandle(name: String, varhandle: VarHandle)

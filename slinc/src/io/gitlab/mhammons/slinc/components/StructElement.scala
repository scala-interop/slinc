package io.gitlab.mhammons.slinc.components

import scala.quoted.Type

final case class StructElement(name: String, typ: Type[?])
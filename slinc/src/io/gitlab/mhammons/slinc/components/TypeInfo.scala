package io.gitlab.mhammons.slinc.components

import scala.quoted.Type

case class StructInfo(
    name: Option[String],
    members: Seq[PrimitiveInfo | StructStub]
)

case class StructStub(name: String, typ: Type[?])

case class PrimitiveInfo(name: String, typ: Type[?])

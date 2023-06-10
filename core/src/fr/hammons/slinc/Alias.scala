package fr.hammons.slinc

import types.{OS, Arch, os, arch}
import scala.reflect.ClassTag

abstract class Alias[T](
    val name: String,
    val aliases: PartialFunction[(OS, Arch), TypeDescriptor]
)(using ClassTag[T])
    extends DescriptorOf[T](
      AliasDescriptor[T](
        aliases.applyOrElse(
          os -> arch,
          _ => throw new Error("")
        )
      )
    )

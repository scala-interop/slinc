package fr.hammons.slinc

import types.{OS, Arch, os, arch}

trait Alias[T] extends DescriptorOf[T]:
  lazy val name: String
  lazy val aliases: PartialFunction[(OS, Arch), TypeDescriptor]
  val descriptor: TypeDescriptor { type Inner >: T <: T } =
    AliasDescriptor[T](
      aliases.applyOrElse(
        os -> arch,
        _ =>
          throw new Error(
            s"Alias for $name is not defined on platform $os - $arch"
          )
      )
    )

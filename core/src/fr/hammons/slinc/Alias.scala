package fr.hammons.slinc

import types.{OS, Arch, os, arch}

trait Alias[T] extends DescriptorOf[T]:
  val name: String
  val aliases: PartialFunction[(OS, Arch), RealTypeDescriptor]
  lazy val descriptor: TypeDescriptor { type Inner >: T <: T } =
    new AliasDescriptor[T](
      aliases.applyOrElse(
        os -> arch,
        _ =>
          throw new Error(
            s"Alias for $name is not defined on platform $os - $arch"
          )
      )
    ):

      val reader = (rwm, _) ?=> (mem, bytes) => rwm.readAlias(mem, bytes, real)
      val writer = (rwm, _) ?=>
        (mem, bytes, a) => rwm.writeAlias(mem, bytes, real, a)

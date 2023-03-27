package fr.hammons.slinc.types

import scala.annotation.nowarn

//todo: remove once https://github.com/lampepfl/dotty/issues/16878 is fixed
@nowarn("msg=unused explicit parameter")
private[slinc] enum Arch:
  case I386
  case X64
  case AArch64
  case Unknown

private[slinc] val arch =
  val archString = System.getProperty("os.arch").nn.toLowerCase()
  val potential = archString.nn match
    case "x86" | "i386" | "i86pc" | "i686" => Arch.I386
    case "x86_64" | "amd64"                => Arch.X64
    case "aarch64"                         => Arch.AArch64
    case arch                              => Arch.Unknown

  if potential == Arch.Unknown then
    Arch.values
      .find(_.productPrefix.toLowerCase() == archString)
      .getOrElse(Arch.Unknown)
  else potential

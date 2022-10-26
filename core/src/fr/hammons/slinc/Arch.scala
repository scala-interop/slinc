package fr.hammons.slinc

private[slinc] enum Arch:
  case I386
  case X64
  case Unknown

private[slinc] val arch =
  val archString = System.getProperty("os.arch").nn.toLowerCase()
  val potential = archString.nn match
    case "x86" | "i386" | "i86pc" | "i686" => Arch.I386
    case "x86_64" | "amd64"                => Arch.X64
    case _                                 => Arch.Unknown

  if potential == Arch.Unknown then
    Arch.values
      .find(_.productPrefix.toLowerCase() == archString)
      .getOrElse(Arch.Unknown)
  else potential

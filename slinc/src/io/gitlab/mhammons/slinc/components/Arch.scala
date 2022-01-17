package io.gitlab.mhammons.slinc.components

/** System arch detection
  * @see
  *   [jnr-ffi](https://github.com/jnr/jnr-ffi/blob/master/src/main/java/jnr/ffi/Platform.java)
  *   for the inspiration for this code
  */
enum Arch:
   case I386
   case X86_64
   case PPC
   case PPC64
   case PPC64LE
   case Sparc
   case SparcV9
   case S390X
   case MIPS32
   case ARM
   case AArch64
   case MIPS64EL
   case Unknown

val arch =
   val archString = System.getProperty("os.arch").toLowerCase
   val potentialArch = archString match
      case "x86" | "i386" | "i86pc" | "i686" => Arch.I386
      case "x86_64" | "amd64"                => Arch.X86_64
      case _                                 => Arch.Unknown

   if potentialArch == Arch.Unknown then
      Arch.values
         .find(_.productPrefix.toLowerCase == archString)
         .getOrElse(Arch.Unknown)
   else potentialArch

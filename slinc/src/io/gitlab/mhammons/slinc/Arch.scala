package io.gitlab.mhammons.slinc

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

object Arch:
   def apply: Arch =
      val archString = System.getProperty("os.arch")
      archString.toLowerCase match
         case "x86" | "i386" | "i86pc" | "i686" => Arch.I386
         case "x86_64" | "amd64"                => Arch.X86_64

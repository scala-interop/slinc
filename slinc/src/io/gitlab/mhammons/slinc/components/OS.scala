package io.gitlab.mhammons.slinc.components

/** OS detection
  * @see
  *   [jnr-ffi](https://github.com/jnr/jnr-ffi/blob/master/src/main/java/jnr/ffi/Platform.java)
  *   for the inspiration for this code
  */
enum OS:
   case Darwin
   case FreeBSD
   case NetBSD
   case OpenBSD
   case Dragonfly
   case Linux
   case Solaris
   case Windows
   case AIX
   case IBMI
   case ZLinux
   case MidnightBSD
   case Unknown

val os =
   val osName = System.getProperty("os.name").split(" ")(0).toLowerCase
   osName match
      case "mac" | "darwin" => OS.Darwin
      case "linux"          => OS.Linux
      case "windows"        => OS.Windows
      case _                => OS.Unknown

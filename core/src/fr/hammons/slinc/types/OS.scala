package fr.hammons.slinc.types

import scala.annotation.nowarn

//todo: remove once https://github.com/lampepfl/dotty/issues/16878 is fixed
@nowarn("msg=unused explicit parameter")
enum OS:
  case Linux
  case Darwin
  case Windows
  case Unknown
object OS:
  extension (platform: OS)
    def sharedObjectSuffix: String = platform match
      case Linux | Darwin => ".so"
      case Windows        => ".dll"
      case Unknown        => ""

  /** @return
    *   [[OS]] inffered from system properties. If it is impossible to infer platform, this method returns [[OS.Unknown]]. 
    */
  def inferred(): OS =
    System.getProperty("os.name") match
      case null => OS.Unknown
      case osname =>
        osname.nn.split(" ").nn(0) match
          case null => OS.Unknown
          case osname =>
            osname.nn.toLowerCase match
              case "mac" | "darwin" => OS.Darwin
              case "linux"          => OS.Linux
              case "windows"        => OS.Windows
              case _                => OS.Unknown

val os =
  val osName = System.getProperty("os.name").nn.split(" ").nn(0).nn.toLowerCase
  osName match
    case "mac" | "darwin" => OS.Darwin
    case "linux"          => OS.Linux
    case "windows"        => OS.Windows
    case _                => OS.Unknown

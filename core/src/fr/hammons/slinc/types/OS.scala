package fr.hammons.slinc.types

import scala.annotation.nowarn

//todo: remove once https://github.com/lampepfl/dotty/issues/16878 is fixed
@nowarn("msg=unused explicit parameter")
enum OS:
  case Linux
  case Darwin
  case Windows
  case Unknown

val os =
  val osName = System.getProperty("os.name").nn.split(" ").nn(0).nn.toLowerCase
  osName match
    case "mac" | "darwin" => OS.Darwin
    case "linux"          => OS.Linux
    case "windows"        => OS.Windows
    case _                => OS.Unknown

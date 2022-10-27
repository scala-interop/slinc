package fr.hammons.slinc.types

private[slinc] enum OS:
  case Linux
  case Darwin
  case Windows
  case Unknown

private[slinc] val os =
  val osName = System.getProperty("os.name").nn.split(" ").nn(0).nn.toLowerCase
  osName match
    case "mac" | "darwin" => OS.Darwin
    case "linux"          => OS.Linux
    case "windows"        => OS.Windows
    case _                => OS.Unknown

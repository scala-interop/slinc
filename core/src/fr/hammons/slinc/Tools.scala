package fr.hammons.slinc

import java.nio.file.{Files, Paths}
import types.{os, OS}
import scala.sys.process.*

object Tools:
  private val appDataStore =
    Paths
      .get(os match
        case OS.Windows =>
          s"${System.getenv("APPDATA").nn}\\local\\slinc\\libstore\\"
        case OS.Linux => s"${System.getProperty("user.home").nn}/.cache/slinc/"
        case OS.Darwin =>
          s"${System.getProperty("user.home").nn}/Library/Application Support/"
        // todo: write a better handling for this
        case OS.Unknown => ???
      )
      .nn

  private val sharedLibSuffix =
    os match
      case OS.Linux | OS.Darwin => ".so"
      case OS.Windows           => ".dll"
      case OS.Unknown =>
        throw Error("Cannot do lib compilation on this platform, it's unknown.")

  def sendResourceToCache(name: String): Unit =
    Files.createDirectories(appDataStore)
    val cacheLocation = appDataStore.resolve(s"$name.c")

    if !Files.exists(appDataStore.resolve(s"$name.c")) then
      val stream = getClass().getResourceAsStream(s"/native/$name.c")
      if stream != null then Files.copy(stream, cacheLocation)
      else throw Error(s"Could not find resource /native/$name.c")

  def compileCachedResourceIfNeeded(name: String): Unit =
    val cacheLocation = appDataStore.resolve(s"$name$sharedLibSuffix")
    val headerLocation = appDataStore.resolve(s"$name.c")

    if !Files.exists(cacheLocation) then
      val cmd = Seq(
        "clang",
        "-shared",
        "-fvisibility=default",
        "-Os",
        "-o",
        cacheLocation.nn.toAbsolutePath().nn.toString(),
        headerLocation.nn.toAbsolutePath().nn.toString()
      )
      if cmd.! != 0 then
        throw Error(s"failed to compile $headerLocation: ${cmd.mkString(" ")}")

  def loadCachedLibrary(name: String) =
    val cacheLocation = appDataStore.resolve(s"$name$sharedLibSuffix")
    System.load(cacheLocation.nn.toAbsolutePath().nn.toString())

end Tools

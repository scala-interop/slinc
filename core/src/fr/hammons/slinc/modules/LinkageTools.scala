package fr.hammons.slinc.modules

import java.nio.file.Paths
import fr.hammons.slinc.types.{OS, os}
import java.nio.file.Files
import java.nio.file.Path
import java.io.InputStream
import java.security.MessageDigest
import java.io.FileInputStream

object LinkageTools:
  private val md =
    ThreadLocal.withInitial(() => MessageDigest.getInstance("SHA256")).nn
  private val cacheLocation = os match
    case OS.Windows =>
      Paths.get(System.getenv("APPDATA"), "local", "slinc", "libstore").nn
    case OS.Linux =>
      Paths.get(System.getProperty("user.home"), ".cache", "slinc").nn
    case OS.Darwin =>
      Paths
        .get(System.getProperty("user.home"), "Library", "Application Support")
        .nn
    case OS.Unknown => ???

  def sendResourceToCache(location: String): String =
    if !Files.exists(cacheLocation) then Files.createDirectories(cacheLocation)
    val cacheFile = cacheLocation.resolve(location)
    val resourceHash = hash(
      getClass().getResourceAsStream(s"/native/$location").nn
    )

    if !Files.exists(cacheFile) || hash(
        FileInputStream(cacheFile.toString())
      ) != resourceHash
    then
      Files.deleteIfExists(cacheFile)
      val stream = getClass().getResourceAsStream(s"/native/$location")
      if stream != null then Files.copy(stream, cacheFile)
      else throw Error(s"Could not find resource /native/$location")

    cacheFile.toString()

  def load(path: Path): Unit =
    System.load(path.toAbsolutePath().toString())

  def hash(stream: InputStream): Seq[Byte] =
    val buffer = Array.ofDim[Byte](1024)
    var len = stream.read(buffer)
    val messageDigest = md.get().nn
    while len != -1 do
      messageDigest.update(buffer)
      len = stream.read(buffer)

    stream.close()
    Seq.from(messageDigest.digest().nn)

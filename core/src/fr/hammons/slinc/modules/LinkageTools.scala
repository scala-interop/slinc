package fr.hammons.slinc.modules

import java.nio.file.Paths
import fr.hammons.slinc.types.{OS, os}
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.io.FileInputStream
import fr.hammons.slinc.fset.Dependency
import java.util.concurrent.atomic.AtomicReference
import java.io.InputStream

import scala.sys.process.*

object LinkageTools:
  private val dependenciesLoaded = AtomicReference(Set.empty[Dependency])

  private val libSuffix = os match
    case OS.Linux | OS.Darwin => ".so"
    case OS.Windows           => ".dll"
    case OS.Unknown => throw Error("Lib suffix is unknown on this platform")

  private val resourcesLocation = "/native"

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
    case OS.Unknown => throw Error("Cache location is unknown on this platform")

  def sendResourceToCache(location: Path): CacheFile =
    if !Files.exists(cacheLocation) then Files.createDirectories(cacheLocation)
    val cacheFile = cacheLocation.resolve(location)
    val resourceHash = hash(
      getClass()
        .getResourceAsStream(s"$resourcesLocation/$location")
        .nn
    )

    if !Files.exists(cacheFile) || hash(
        FileInputStream(cacheFile.toString())
      ) != resourceHash
    then
      Files.deleteIfExists(cacheFile)
      val stream = getClass().getResourceAsStream(s"/native/$location")
      if stream != null then Files.copy(stream, cacheFile)
      else throw Error(s"Could not find resource /native/$location")

      CacheFile(location, cacheFile.nn, true)
    else CacheFile(location, cacheFile.nn, false)

  def load(path: Path): Unit =
    System.load(path.toAbsolutePath().toString())

  def loadDependency(dependency: Dependency): Unit = synchronized {
    val currentDeps = dependenciesLoaded.get.nn
    if !currentDeps.contains(dependency) then
      dependency match
        case Dependency.LibraryResource(path) =>
          val cachedFile = sendResourceToCache(path)
          load(cachedFile.cachePath)
        case Dependency.CResource(path) =>
          val cachedFile = sendResourceToCache(path)
          val compilationPath = compileCachedCCode(cachedFile)
          load(compilationPath)

      dependenciesLoaded.compareAndExchange(
        currentDeps,
        currentDeps + dependency
      )
  }

  def compileCachedCCode(cachedFile: CacheFile): Path =
    val libLocation = cachedFile.cachePath.toString() match
      case s"${baseName}.c" => Paths.get(s"$baseName$libSuffix").nn

    if !Files.exists(libLocation) || cachedFile.updated then
      val cmd = Seq(
        "clang",
        "-shared",
        "-fvisibility=default",
        "-Os",
        "-o",
        libLocation.nn.toAbsolutePath().nn.toString(),
        cachedFile.cachePath.toAbsolutePath().nn.toString()
      )
      if cmd.! != 0 then
        throw Error(
          s"failed to compile resource ${cachedFile.origin.toAbsolutePath()}"
        )

    libLocation

  def hash(stream: InputStream): Seq[Byte] =
    val buffer = Array.ofDim[Byte](1024)
    var len = stream.read(buffer)
    val messageDigest = md.get().nn
    while len != -1 do
      messageDigest.update(buffer)
      len = stream.read(buffer)

    stream.close()
    Seq.from(messageDigest.digest().nn)

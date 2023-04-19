package fr.hammons.slinc.modules

import java.nio.file.Paths
import fr.hammons.slinc.types.{OS, os}
import fr.hammons.slinc.types.{Arch, arch}
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

  private lazy val libSuffix = os match
    case OS.Linux | OS.Darwin => ".so"
    case OS.Windows           => ".dll"
    case OS.Unknown => throw Error("Lib suffix is unknown on this platform")

  private lazy val potentialArchMarks = arch match
    case Arch.X64         => Set("x64", "x86_64", "amd64")
    case Arch.Unknown | _ => Set.empty

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

  def sendResourceToCache(location: String): CacheFile =
    val resourceStream =
      getClass().getResourceAsStream(s"$resourcesLocation/$location")
    if resourceStream == null then
      throw Error(s"Could not find a resource like $location!!")
    else
      if !Files.exists(cacheLocation) then
        Files.createDirectories(cacheLocation)
      val cacheFile = cacheLocation.resolve(location)
      val resourceHash = hash(
        resourceStream
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

  def loadLibrary(name: String): Unit =
    System.loadLibrary(name)

  def loadDependency(dependency: Dependency): Unit = synchronized {
    val currentDeps = dependenciesLoaded.get.nn
    if !currentDeps.contains(dependency) then
      dependency match
        case Dependency.LibraryResource(path, true) =>
          val cachedFile = sendResourceToCache(path)
          load(cachedFile.cachePath)

        case Dependency.LibraryResource(path, false) =>
          val resolvedPath = potentialArchMarks.view
            .map(archMark => s"${path}_$archMark$libSuffix")
            .find(path =>
              getClass().getResource(s"$resourcesLocation/$path") != null
            )
            .getOrElse(
              throw Error(
                s"No library resource found for $arch $os with path like $path"
              )
            )

          val cachedFile = sendResourceToCache(resolvedPath)
          load(cachedFile.cachePath)

        case Dependency.CResource(path) =>
          val cachedFile = sendResourceToCache(path)
          val compilationPath = compileCachedCCode(cachedFile)
          load(compilationPath)
        case Dependency.PathLibrary(name) =>
          loadLibrary(name)
        case Dependency.FilePath(path, true) =>
          load(path)

        case Dependency.FilePath(path, false) =>
          val resolvedPath = potentialArchMarks.view
            .map(archMark => Paths.get(s"${path}_$archMark$libSuffix").nn)
            .find(Files.exists(_))
            .getOrElse(
              throw Error(
                s"No library file found for $arch $os with path like $path"
              )
            )

          load(resolvedPath)

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
        "-v",
        "-shared",
        "-fvisibility=default",
        "-Os",
        "-o",
        libLocation.nn.toAbsolutePath().nn.toString(),
        cachedFile.cachePath.toAbsolutePath().nn.toString()
      )
      if cmd.! != 0 then
        throw Error(
          s"failed to compile resource ${cachedFile.origin}"
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

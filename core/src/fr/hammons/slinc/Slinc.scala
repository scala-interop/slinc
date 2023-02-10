package fr.hammons.slinc

import scala.util.chaining.*
import scala.compiletime.uninitialized
import modules.{DescriptorModule, TransitionModule}
import fr.hammons.slinc.modules.ReadWriteModule

trait Slinc:
  protected def jitManager: JitManager

  protected def scopePlatformSpecific: ScopeI.PlatformSpecific
  protected def libraryIPlatformSpecific: LibraryI.PlatformSpecific

  given dm: DescriptorModule
  given tm: TransitionModule
  given rwm: ReadWriteModule

  protected val structI = StructI(jitManager)
  val typesI = types.TypesI.platformTypes
  protected val scopeI = ScopeI(scopePlatformSpecific)
  protected val libraryI = LibraryI(libraryIPlatformSpecific)
  val receiveI = ReceiveI(libraryIPlatformSpecific)

  export typesI.{*, given}
  export libraryI.*
  export Convertible.as
  export PotentiallyConvertible.maybeAs
  export structI.Struct
  export scopeI.given
  export container.ContextProof.given
  export receiveI.given

  object x64:
    val Linux: types.x64.Linux.type = types.x64.Linux
    val Mac: types.x64.Mac.type = types.x64.Mac
    val Windows: types.x64.Windows.type = types.x64.Windows

  export types.os

  def sizeOf[A](using l: DescriptorOf[A]) =
    DescriptorOf[A].size.toLong.maybeAs[SizeT]

  def Null[A] = scopePlatformSpecific.nullPtr[A]

  extension (l: Long) def toBytes = Bytes(l)

object Slinc:
  private val majorVersion = System
    .getProperty("java.version")
    .nn
    .takeWhile(_.isDigit)
    .pipe(vString =>
      vString.toIntOption.getOrElse(
        throw Error(
          s"Major error occured. Couldn't parse the version number $vString"
        )
      )
    )

  @volatile private var _runtime: Slinc | Null = uninitialized

  inline def getRuntime(): Slinc =
    if _runtime == null then
      _runtime = SlincImpl
        .findImpls()
        .getOrElse(
          majorVersion,
          throw Error(
            s"Sorry, an implementation for JVM $majorVersion couldn't be found. Please check that you've added dependencies on slinc-j$majorVersion, if it exists..."
          )
        )()
      _runtime.asInstanceOf[Slinc]
    else _runtime.asInstanceOf[Slinc]

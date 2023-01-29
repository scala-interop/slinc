package fr.hammons.slinc

import scala.concurrent.ExecutionContext
import scala.quoted.staging.Compiler
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import scala.util.chaining.*
import java.util.concurrent.atomic.AtomicReference
import scala.compiletime.uninitialized
import modules.DescriptorModule

trait Slinc:
  protected def jitManager: JitManager

  protected def layoutPlatformSpecific: LayoutI.PlatformSpecific
  protected def scopePlatformSpecific: ScopeI.PlatformSpecific
  protected def transitionsPlatformSpecific: TransitionsI.PlatformSpecific
  protected def libraryIPlatformSpecific: LibraryI.PlatformSpecific

  given dm: DescriptorModule

  private val useJit = Option(System.getProperty("sffi-jit"))
    .flatMap(_.nn.toBooleanOption)
    .getOrElse(true)
  protected val layoutI = LayoutI(layoutPlatformSpecific)
  protected val transitionsI = TransitionsI(transitionsPlatformSpecific)
  protected val structI = StructI(layoutI, transitionsI, jitManager)
  val typesI = types.TypesI.platformTypes(layoutI)
  protected val scopeI = ScopeI(scopePlatformSpecific)
  protected val libraryI = LibraryI(libraryIPlatformSpecific)
  val receiveI = ReceiveI(libraryIPlatformSpecific, layoutI)

  export layoutI.{*, given}
  export typesI.{*, given}
  export libraryI.*
  export Convertible.as
  export PotentiallyConvertible.maybeAs
  export transitionsI.given
  export structI.Struct
  export scopeI.given
  export container.ContextProof.given
  export receiveI.given

  object x64:
    val Linux: types.x64.Linux.type = types.x64.Linux
    val Mac: types.x64.Mac.type = types.x64.Mac
    val Windows: types.x64.Windows.type = types.x64.Windows

  export types.os

  def sizeOf[A](using l: DescriptorOf[A]) = DescriptorOf[A].size.toLong.maybeAs[SizeT]

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

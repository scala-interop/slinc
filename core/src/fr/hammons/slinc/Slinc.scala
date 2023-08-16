package fr.hammons.slinc

import scala.util.chaining.*
import scala.compiletime.uninitialized
import fr.hammons.slinc.modules.*
import types.SizeT

trait Slinc:
  val version: Int
  protected def scopePlatformSpecific: ScopeI.PlatformSpecific

  given dm: DescriptorModule
  given tm: TransitionModule
  given rwm: ReadWriteModule
  given lm: FSetModule

  protected val scopeI = ScopeI(scopePlatformSpecific)

  export scopeI.given
  export container.ContextProof.given

  export types.os

  def sizeOf[A](using l: DescriptorOf[A]) =
    SizeT.maybe(DescriptorOf[A].toForeignTypeDescriptor.size.toLong).get

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

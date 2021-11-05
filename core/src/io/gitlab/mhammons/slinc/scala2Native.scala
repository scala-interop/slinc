package io.gitlab.mhammons.slinc

import scala.compiletime.erasedValue
import jdk.incubator.foreign.MemoryAddress
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.MemorySegment
import scala.compiletime.summonInline
import scala.compiletime.asMatchable
import jdk.incubator.foreign.MemoryLayout
import scala.compiletime.constValue

trait Struk
trait Field
trait -->[S <: Singleton, V] extends Struk, Field
trait ~%[R <: Struk, F <: Field] extends Struk

class GLBuilder(list: List[(String, MemoryLayout)])

inline def scala2MethodTypeArg[A] =
   inline erasedValue[A] match
      case _: Long          => classOf[Long]
      case _: Int           => classOf[Int]
      case _: Ptr[?]        => classOf[MemoryAddress]
      case _: MemoryAddress => classOf[MemoryAddress]
      case _: Struk         => classOf[MemorySegment]
      case _: Fd.int        => classOf[Int]
      case _: Unit          => VoidHelper.v
inline def struk2GroupLayoutPieces[A]: Seq[MemoryLayout] =
   inline erasedValue[A] match
      case _: ~%[rest, -->[s, v]] =>
         struk2GroupLayoutPieces[rest] :+ scala2MemoryLayout[v]
            .withName(constValue[s].toString)
      case _: -->[s, v] =>
         Seq(scala2MemoryLayout[v].withName(constValue[s].toString))
inline def scala2MemoryLayout[A]: MemoryLayout =
   inline erasedValue[A] match
      case _: Long          => CLinker.C_LONG
      case _: Int           => CLinker.C_INT
      case _: Ptr[t]        => CLinker.C_POINTER
      case _: MemoryAddress => CLinker.C_POINTER

transparent inline def returnType[A](result: Any) =
   inline erasedValue[A] match
      case _: Struk => result.asInstanceOf[MemorySegment]
      case _        => result.asInstanceOf[A]

type tm = ("hello" --> Int) ~%
   ("world" --> Float)

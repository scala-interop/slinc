package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import scala.compiletime.{erasedValue, summonInline, summonFrom}

trait Downcall[T <: Tuple, R]:
  val mh: MethodHandle

class DowncallI(platform: DowncallI.PlatformSpecific):

  inline given idc[T <: Tuple, R]: Downcall[T, R] = new Downcall[T, R]:
    val mh =
      platform.getDowncall(
        getDowncallHelper[T],
        summonFrom {
          case dO: DescriptorOf[R] => Some(dO.descriptor)
          case _                   => None
        }
      )

  private inline def getDowncallHelper[T <: Tuple]: Seq[TypeDescriptor] =
    inline erasedValue[T] match
      case _: (h *: t) =>
        summonInline[DescriptorOf[h]].descriptor +: getDowncallHelper[t]
      case _: EmptyTuple => Seq.empty

object DowncallI:
  trait PlatformSpecific:
    def getDowncall(
        layout: Seq[TypeDescriptor],
        ret: Option[TypeDescriptor]
    ): MethodHandle

package fr.hammons.slinc

import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.compiletime.summonInline
import fr.hammons.slinc.modules.ReadWriteModule
import fr.hammons.slinc.modules.DescriptorModule
import scala.NonEmptyTuple

class CUnion[T <: Tuple](private[slinc] val mem: Mem):
  private inline def getHelper[T <: Tuple, A](using
      dO: DescriptorOf[A],
      rwm: ReadWriteModule
  ): A =
    inline erasedValue[T] match
      case _: (A *: ?)   => rwm.read(mem, Bytes(0), dO.descriptor)
      case _: (? *: t)   => getHelper[t, A]
      case _: EmptyTuple => error("Cannot extract this type from union")

  private inline def setHelper[T <: Tuple, A](
      a: A
  )(using dO: DescriptorOf[A], rwm: ReadWriteModule): Unit =
    inline erasedValue[T] match
      case _: (A *: ?)   => rwm.write(mem, Bytes(0), dO.descriptor, a)
      case _: (? *: t)   => setHelper[t, A](a)
      case _: EmptyTuple => error("Cannot set value of this type to this union")

  inline def get[A](using DescriptorOf[A], ReadWriteModule): A = getHelper[T, A]

  inline def set[A](a: A)(using DescriptorOf[A], ReadWriteModule): Unit =
    setHelper[T, A](a)

object CUnion:
  private inline def applyHelper[T <: Tuple](td: TypeDescriptor | Null)(using
      DescriptorModule
  ): TypeDescriptor = inline erasedValue[T] match
    case _: (a *: t) =>
      val aDesc = summonInline[DescriptorOf[a]].descriptor
      val max =
        if td != null then
          if td.size > aDesc.size then td
          else aDesc
        else aDesc

      applyHelper[t](max)

    case EmptyTuple => td.nn

  inline def apply[T <: NonEmptyTuple](using
      is: InferredScope,
      descriptorModule: DescriptorModule
  ) =
    val maxDescriptor = applyHelper[T](null).nn
    is { allocator ?=>
      new CUnion[T](allocator.allocate(maxDescriptor, 1))
    }

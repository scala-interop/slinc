package fr.hammons.slinc.types

import fr.hammons.slinc.Alias
import fr.hammons.slinc.LongDescriptor
import fr.hammons.slinc.ByteDescriptor
import fr.hammons.slinc.ShortDescriptor
import fr.hammons.slinc.IntDescriptor

object IntegralAlias:
  def range[T](using a: Alias[T]) = a.aliases.applyOrElse(
    (os, arch),
    _ => throw new Error(s"Alias for ${a.name} not defined for this platform")
  ) match
    case ByteDescriptor  => Byte.MinValue to Byte.MaxValue
    case ShortDescriptor => Short.MinValue to Short.MaxValue
    case IntDescriptor   => Int.MinValue to Int.MaxValue
    case LongDescriptor  => Long.MinValue to Long.MaxValue
    case _ =>
      throw new Error(
        s"${a.name} is not an alias for an integral type on $os $arch"
      )

  def transform[T]: Transform[T] = Transform[T]
  class Transform[T]:
    def apply[U](value: U)(using a: Alias[T], n: Numeric[U]) =
      a.aliases.applyOrElse(
        (os, arch),
        _ =>
          throw new Error(s"Alias for ${a.name} not defined for this platform")
      ) match
        case ByteDescriptor  => n.toInt(value).toByte
        case ShortDescriptor => n.toInt(value).toShort
        case IntDescriptor   => n.toInt(value)
        case LongDescriptor  => n.toLong(value)
        case _ =>
          throw new Error(
            s"${a.name} is not an alias for an integral type on $os - $arch"
          )

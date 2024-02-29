package fr.hammons.slinc.types

import fr.hammons.slinc.Alias
import fr.hammons.slinc.LongDescriptor
import fr.hammons.slinc.ByteDescriptor
import fr.hammons.slinc.ShortDescriptor
import fr.hammons.slinc.IntDescriptor

object IntegralAlias:
  def min[T](using a: Alias[T]): Long = a.aliases.applyOrElse(
    (os, arch),
    _ => throw new Error(s"Alias for ${a.name} not defined for this platform")
  ) match
    case ByteDescriptor  => Byte.MinValue.toLong
    case ShortDescriptor => Short.MinValue.toLong
    case IntDescriptor   => Int.MinValue.toLong
    case LongDescriptor  => Long.MinValue
    case _ =>
      throw new Error(
        s"${a.name} is not an alias for an integral type on $os $arch"
      )

  def max[T](using a: Alias[T]): Long = a.aliases.applyOrElse(
    (os, arch),
    _ => throw new Error(s"Alias for ${a.name} not defined for this platform")
  ) match
    case ByteDescriptor  => Byte.MaxValue.toLong
    case ShortDescriptor => Short.MaxValue.toLong
    case IntDescriptor   => Int.MaxValue.toLong
    case LongDescriptor  => Long.MaxValue
    case _ =>
      throw new Error(
        s"${a.name} is not an alias for an integral type on $os $arch"
      )

  def range[T](using a: Alias[T]) = a.aliases.applyOrElse(
    (os, arch),
    _ => throw new Error(s"Alias for ${a.name} not defined for this platform")
  ) match
    case ByteDescriptor => Range.Long.inclusive(Byte.MinValue, Byte.MaxValue, 1)
    case ShortDescriptor =>
      Range.Long.inclusive(Short.MinValue, Short.MaxValue, 1)
    case IntDescriptor  => Range.Long.inclusive(Int.MinValue, Int.MaxValue, 1)
    case LongDescriptor => Long.MinValue to Long.MaxValue
    case _ =>
      throw new Error(
        s"${a.name} is not an alias for an integral type on $os $arch"
      )

  def toLong[T](value: T)(using a: Alias[T]) = a.aliases.applyOrElse(
    (os, arch),
    _ => throw new Error(s"Alias for ${a.name} not defined for this platform")
  ) match
    case ByteDescriptor  => value.asInstanceOf[Byte].toLong
    case ShortDescriptor => value.asInstanceOf[Short].toLong
    case IntDescriptor   => value.asInstanceOf[Int].toLong
    case LongDescriptor  => value.asInstanceOf[Long].toLong
    case _ =>
      throw new Error(
        s"${a.name} is not an alias for an integral type on $os - $arch"
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

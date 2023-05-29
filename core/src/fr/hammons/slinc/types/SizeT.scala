package fr.hammons.slinc.types

import fr.hammons.slinc.Alias
import fr.hammons.slinc.LongDescriptor

opaque type SizeT = AnyVal

object SizeT:
  def apply(value: Short | Byte): SizeT = value match
    case s: Short => IntegralAlias.transform[SizeT](s)
    case b: Byte  => IntegralAlias.transform[SizeT](b)

  def maybe(maybeValue: Int | Long): Option[SizeT] =
    val value = maybeValue match
      case i: Int  => i.toLong
      case l: Long => l.toLong

    if value < 65536 || IntegralAlias.range[SizeT].contains(value) then
      Some(IntegralAlias.transform[SizeT](value))
    else None

  given Alias[SizeT](
    "SizeT",
    { case (OS.Linux | OS.Darwin | OS.Windows, Arch.X64 | Arch.AArch64) =>
      LongDescriptor
    }
  ) with {}

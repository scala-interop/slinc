package fr.hammons.slinc.types

import fr.hammons.slinc.LongDescriptor
import fr.hammons.slinc.IntDescriptor
import fr.hammons.slinc.Alias

opaque type CLong = AnyVal

object CLong:
  def apply(fitsForSure: Byte | Short | Int): CLong =
    fitsForSure match
      case b: Byte  => IntegralAlias.transform[CLong](b)
      case s: Short => IntegralAlias.transform[CLong](s)
      case i: Int   => IntegralAlias.transform[CLong](i)

  def maybe(maybeFits: Long): Option[CLong] =
    if (maybeFits <= Int.MaxValue && maybeFits >= Int.MinValue) || (IntegralAlias
        .min[CLong] <= maybeFits && maybeFits <= IntegralAlias.max[CLong])
    then Some(IntegralAlias.transform[CLong](maybeFits))
    else None

  given Alias[CLong] with
    lazy val name = "CLong"
    lazy val aliases = {
      case (OS.Linux | OS.Darwin, Arch.X64 | Arch.AArch64) => LongDescriptor
      case (OS.Windows, Arch.X64)                          => IntDescriptor
    }

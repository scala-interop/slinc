package fr.hammons.slinc.types

import fr.hammons.slinc.Alias
import fr.hammons.slinc.LongDescriptor

opaque type TimeT = Any

given Alias[TimeT] with
  val name: String = "TimeT"
  val aliases = { case (OS.Windows | OS.Linux | OS.Darwin, Arch.X64) =>
    LongDescriptor
  }

object TimeT:
  def maybe(value: Byte | Short | Int | Long): Option[TimeT] =
    val upcast = value match
      case b: Byte  => b.toLong
      case s: Short => s.toLong
      case i: Int   => i.toLong
      case l: Long  => l.toLong

    if IntegralAlias.range[TimeT].contains(upcast) then
      Some(IntegralAlias.transform[TimeT](upcast))
    else None

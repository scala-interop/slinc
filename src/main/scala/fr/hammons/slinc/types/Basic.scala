package fr.hammons.slinc.types

import fr.hammons.slinc.Transform

type CChar = Byte
type CShort = Short
type CInt = Int
type CFloat = Float
type CDouble = Double
type CLongLong = Long

opaque type CBool >: Boolean <: Boolean = Boolean

object CBool:
  given Transform[CBool, Byte](
    b => if b != (0: Byte) then true else false,
    b => if b then 1: Byte else 0: Byte
  )

opaque type CBoolShort >: Boolean <: Boolean = Boolean

object CBoolShort:
  given Transform[CBoolShort, Short](
    s => if s != (0: Short) then true else false,
    b => if b then 1: Short else 0: Short
  )

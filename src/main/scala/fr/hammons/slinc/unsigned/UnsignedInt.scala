package fr.hammons.slinc.unsigned

opaque type UnsignedInt = Int

object UnsignedInt:
  extension (ui: UnsignedInt)
    def >(other: UnsignedInt) = Integer.compareUnsigned(ui, other) == 1
    def <(other: UnsignedInt) = Integer.compareUnsigned(ui, other) == -1
    def >=(other: UnsignedInt) = Integer.compareUnsigned(ui, other) > -1
    def <=(other: UnsignedInt) = Integer.compareUnsigned(ui, other) < 1
    def ==(other: UnsignedInt) = Integer.compareUnsigned(ui, other) == 0

    def /(other: UnsignedInt) = Integer.divideUnsigned(ui, other)
    def +(other: UnsignedInt) = ui + other
    def -(other: UnsignedInt) = ui - other
    def *(other: UnsignedInt) = ui * other

    def toString = Integer.toUnsignedString(ui).nn

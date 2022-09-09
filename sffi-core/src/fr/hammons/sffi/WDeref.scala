package fr.hammons.sffi

trait WDeref: 
  self: WBasics =>
  trait Deref[A]:
    def deref(mem: RawMem, offset: Long): A
    def toArray(mem: RawMem, offset: Long, elements: Long): Array[A]

  given intDeref: Deref[Int]
  given byteDeref: Deref[Byte]

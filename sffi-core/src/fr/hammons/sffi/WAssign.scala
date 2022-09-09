package fr.hammons.sffi

trait WAssign:
  self: WBasics =>
  trait Assign[A]:
    def assign(mem: RawMem, offset: Long, a: A): Unit

  given intAssign: Assign[Int]

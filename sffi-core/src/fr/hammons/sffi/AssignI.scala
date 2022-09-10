package fr.hammons.sffi

trait AssignI[RawMem]:
  trait Assign[A]:
    def assign(mem: RawMem, offset: Long, a: A): Unit 

  given intAssign: Assign[Int]
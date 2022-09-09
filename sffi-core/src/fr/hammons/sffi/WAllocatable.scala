package fr.hammons.sffi

import scala.annotation.targetName

trait WAllocatable:
  self: WBasics =>
  trait Allocatable[A]:
    def apply(
        a: A
    )(using Scope, Allocator): Pointer[A]

    @targetName("arrayApply")
    def apply(
        a: Array[A]
    )(using Scope, Allocator): Pointer[A]

  given intIsAllocatable: Allocatable[Int]
  given byteIsAllocatable: Allocatable[Byte]



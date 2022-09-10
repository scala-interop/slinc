package fr.hammons.sffi

import scala.annotation.targetName

trait AllocatableI[Scope, Allocator, RawMem]:
  trait Allocatable[A]:
    def apply(a: A)(using Scope, Allocator): RawMem
    @targetName("arrayApply")
    def apply(a: Array[A])(using Scope, Allocator): RawMem

  given intIsAllocatable: Allocatable[Int]
  given byteIsAllocatable: Allocatable[Byte]
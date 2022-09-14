package fr.hammons.sffi

import fr.hammons.sffi.Allocate.PlatformSpecific

trait Allocate[RawMem, Allocator, Scope](platformSpecific: PlatformSpecific[RawMem, Allocator,Scope])

object Allocate:
  trait PlatformSpecific[RawMem, Allocator, Scope]:
    def allocateInt(value: Int)(using Allocator): RawMem
    def allocateFloat(value: Float)(using Allocator): RawMem
    def allocateLong(value: Long)(using Allocator): RawMem
    def allocateByte(value: Byte)(using Allocator): RawMem
    
    def allocateIntArray(value: Array[Int])(using Allocator): RawMem
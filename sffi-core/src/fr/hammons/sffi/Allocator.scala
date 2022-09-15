package fr.hammons.sffi

trait Allocator:
  def allocate(layout: DataLayout): Mem
  def upcall[Fn](function: Fn): Mem

object Allocator:
  trait PlatformSpecific:
    def tempAllocator(): Allocator
    def globalAllocator(): Allocator
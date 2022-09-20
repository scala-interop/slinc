package fr.hammons.slinc

trait Allocator:
  def allocate(layout: DataLayout): Mem
  def upcall[Fn](function: Fn): Mem
  def close(): Unit

object Allocator:
  trait PlatformSpecific:
    def tempAllocator(): Allocator
    def globalAllocator(): Allocator
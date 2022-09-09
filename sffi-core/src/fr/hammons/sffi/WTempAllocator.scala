package fr.hammons.sffi

trait WTempAllocator: 
  self: WBasics =>
  
  protected def localAllocator(): Allocator

  protected def resetAllocator(): Unit
  
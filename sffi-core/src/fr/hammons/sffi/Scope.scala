package fr.hammons.sffi

trait ScopeSingleton[Scope, Allocator]:
  def apply[T](
      allocatorType: AllocatorType = AllocatorType.Arena,
      shared: Boolean = false,
      global: Boolean = false
  )(code: Scope ?=> Allocator ?=> T): T

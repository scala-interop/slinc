package fr.hammons.sffi

trait WLookup:
  self: WBasics =>
  type Lookup 
  type Symbol

  def loaderLookup: Lookup 
  extension (l: Lookup)
    def lookup(methodName: String): Symbol
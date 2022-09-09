package fr.hammons.sffi

trait UnionI {
  self: WBasics & WLayoutInfo =>
  
  class Union[T <: Tuple](rawMem: RawMem): 
    ???
  
}

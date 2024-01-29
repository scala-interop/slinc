package fr.hammons.slinc

import fr.hammons.slinc.internal.ast.StackAlloc

type ValidStruct = Struct {
  val a: >[Int]
  val b: >[Double]
}

type InvalidStruct = Struct {
  val a: Int
}

// val a = StackAlloc[ValidStruct]
//val b = StackAlloc[InvalidStruct]

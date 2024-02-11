package fr.hammons.slinc

import fr.hammons.slinc.internal.ast.StackAlloc
import fr.hammons.slinc.internal.IsValidStruct

type ValidStruct = Struct {
  val a: >[Int]
  val b: >[Double]
}

type InvalidStruct = Struct {
  val a: Int
}

type InvalidStruct2 = Struct {
  val a: >[Object]
  val b: Float
}
val r = summon[IsValidStruct[ValidStruct & Struct]]

given Platform = ???
//val a = stackAlloc[ValidStruct]
//val b = stackAlloc[InvalidStruct2]
//val b = StackAlloc[InvalidStruct2]

//val b = StackAlloc[InvalidStruct]

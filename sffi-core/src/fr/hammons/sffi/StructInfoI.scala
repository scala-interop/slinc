package fr.hammons.sffi

trait StructInfoI[LayoutInfo[A <: AnyKind] <: LayoutInfoI[?]#LayoutInfo[A]]:
  trait StructInfo[A](l: LayoutInfo[A]):
    export l.*
    val offsets: IArray[Long]
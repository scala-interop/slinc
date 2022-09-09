package fr.hammons.sffi

import scala.deriving.Mirror

trait WStructInfo: 
  self: WBasics & WLayoutInfo =>

  trait StructInfo[A] extends LayoutInfo[A]:
    val context: Context 
    val offsets: IArray[Long]
    val size: Long
package fr.hammons.sffi

trait LayoutInfoI[Context]:
  trait LayoutInfo[A <: AnyKind]:
    val context: Context 
    val size: Long

  object LayoutInfo:
    def apply[A](using li: LayoutInfo[A]) = li
  
  given intInfo: LayoutInfo[Int]
  given floatInfo: LayoutInfo[Float]
  given longInfo: LayoutInfo[Long]
  given byteInfo: LayoutInfo[Byte]

  def carrierFromContext(c: Context): Class[?]

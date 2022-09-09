package fr.hammons.sffi

import java.lang.invoke.MethodType

trait WLayoutInfo:
  self: WBasics => 

  trait LayoutInfo[A <: AnyKind]:
    val context: Context 
    val size: Long

  object LayoutInfo:
    def apply[A](using li: LayoutInfo[A]) = li
  
  given intInfo: LayoutInfo[Int]
  given floatInfo: LayoutInfo[Float]
  given longInfo: LayoutInfo[Long]
  given byteInfo: LayoutInfo[Byte]
  given ptrInfo: LayoutInfo[Pointer]
  //todo: maybe a match type that reduces all Pointer[A] to Pointer for LayoutInfo?
  given refinedPtrInfo[A]: LayoutInfo[Pointer[A]] = ptrInfo.asInstanceOf[LayoutInfo[Pointer[A]]]

  protected def carrierFromContext(c: Context): Class[?]

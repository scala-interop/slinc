package fr.hammons.sffi

import jdk.incubator.foreign.{CLinker, GroupLayout, MemoryAddress, MemorySegment}

trait LayoutInfo17 extends WLayoutInfo:
  self: Basics17 =>

  given intInfo: LayoutInfo[Int] with 
    val context = CLinker.C_INT.nn
    val size = context.byteSize

  given floatInfo: LayoutInfo[Float] with 
    val context = CLinker.C_FLOAT.nn
    val size = context.byteSize()

  given longInfo: LayoutInfo[Long] with 
    val context = CLinker.C_LONG.nn
    val size = context.byteSize()

  given byteInfo: LayoutInfo[Byte] with 
    val context = CLinker.C_CHAR.nn
    val size = context.byteSize()

  given ptrInfo: LayoutInfo[Pointer] with 
    val context = CLinker.C_POINTER.nn
    val size = context.byteSize()


  protected def carrierFromContext(context: Context): Class[?] =
    import CLinker.*
    context match
      case C_CHAR         => classOf[Byte]
      case C_DOUBLE       => classOf[Double]
      case C_FLOAT        => classOf[Float]
      case C_INT          => classOf[Int]
      case C_LONG         => classOf[Long]
      case C_POINTER      => classOf[MemoryAddress]
      case _: GroupLayout => classOf[MemorySegment]
      case _              => throw new Exception("I shouldn't be here!!!")

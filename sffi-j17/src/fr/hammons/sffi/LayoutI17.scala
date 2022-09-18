package fr.hammons.sffi

import scala.deriving.Mirror
import scala.reflect.ClassTag

object LayoutI17 extends LayoutI.PlatformSpecific:

  override given byteLayout: LayoutOf[Byte] with
    val layout = ByteLayout17()

  override given floatLayout: LayoutOf[Float] = ???

  override given intLayout: LayoutOf[Int] with
    val layout = IntLayout17()

  override def getStructLayout[T](layouts: DataLayout*)(using Mirror.ProductOf[T], ClassTag[T]): StructLayout =
    StructLayout17[T](layouts*)

  override given longLayout: LayoutOf[Long] with 
    val layout = LongLayout17()

  override given shortLayout: LayoutOf[Short] = ???

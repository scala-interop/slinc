package fr.hammons.sffi

object LayoutI17 extends LayoutI.PlatformSpecific:

  override given byteLayout: LayoutOf[Byte] with
    val layout = ByteLayout17()

  override given floatLayout: LayoutOf[Float] = ???

  override given intLayout: LayoutOf[Int] with
    val layout = IntLayout17()

  override def structLayout(layouts: DataLayout*): StructLayout =
    StructLayout17(layouts*)

  override given longLayout: LayoutOf[Long] = ???

  override given shortLayout: LayoutOf[Short] = ???

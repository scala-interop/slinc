package fr.hammons.sffi

import scala.compiletime.summonInline
trait LayoutOf[A]:
  val layout: DataLayout

class LayoutI(protected val platformSpecific: LayoutI.PlatformSpecific):
  export platformSpecific.{given, *}
  given LayoutOf[Char] with
    val layout = shortLayout.layout


object LayoutI:
  trait PlatformSpecific:
    given intLayout: LayoutOf[Int]
    given longLayout: LayoutOf[Long]
    given floatLayout: LayoutOf[Float]
    given shortLayout: LayoutOf[Short]
    given byteLayout: LayoutOf[Byte]
    def structLayout(layouts: DataLayout*): StructLayout

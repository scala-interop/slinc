package fr.hammons.sffi

import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.deriving.Mirror
import scala.reflect.ClassTag.apply
import scala.reflect.ClassTag
trait LayoutOf[A]:
  val layout: DataLayout

class LayoutI(protected val platformSpecific: LayoutI.PlatformSpecific):
  export platformSpecific.given
  import platformSpecific.getStructLayout
  given LayoutOf[Char] with
    val layout = shortLayout.layout

  inline def structLayout[P <: Product](using m: Mirror.ProductOf[P], ct: ClassTag[P]) = 
    getStructLayout[P](
      structLayoutHelper[Tuple.Zip[m.MirroredElemLabels, m.MirroredElemTypes]]*
    )


  inline def structLayoutHelper[T <: Tuple]: List[DataLayout] = 
    inline erasedValue[T] match 
        case _: ((name, value) *: t) =>
          summonInline[LayoutOf[value]].layout
            .withName(constValue[name].toString) :: structLayoutHelper[t]
        case _: EmptyTuple => Nil



object LayoutI:
  trait PlatformSpecific:
    given intLayout: LayoutOf[Int]
    given longLayout: LayoutOf[Long]
    given floatLayout: LayoutOf[Float]
    given shortLayout: LayoutOf[Short]
    given byteLayout: LayoutOf[Byte]
    def getStructLayout[T](layouts: DataLayout*)(using Mirror.ProductOf[T], scala.reflect.ClassTag[T]): StructLayout

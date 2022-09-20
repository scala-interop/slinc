package fr.hammons.slinc

import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.deriving.Mirror
import scala.reflect.ClassTag

trait LayoutOf[A <: AnyKind]:
  val layout: DataLayout

class LayoutI(platformSpecific: LayoutI.PlatformSpecific):
  import platformSpecific.getStructLayout
  given LayoutOf[Char] with
    val layout = platformSpecific.shortLayout

  given LayoutOf[Int] with 
    val layout = platformSpecific.intLayout
  
  given LayoutOf[Long] with 
    val layout = platformSpecific.longLayout
  
  given LayoutOf[Float] with
    val layout = platformSpecific.floatLayout
  
  given LayoutOf[Short] with 
    val layout = platformSpecific.shortLayout

  given LayoutOf[Byte] with 
    val layout = platformSpecific.byteLayout

  given LayoutOf[Ptr] with 
    val layout = platformSpecific.pointerLayout


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
    val intLayout: IntLayout
    val longLayout: LongLayout
    val floatLayout: FloatLayout
    val shortLayout: ShortLayout 
    val doubleLayout: DoubleLayout
    val pointerLayout: PointerLayout
    val byteLayout: ByteLayout
    def getStructLayout[T](layouts: DataLayout*)(using Mirror.ProductOf[T], scala.reflect.ClassTag[T]): StructLayout

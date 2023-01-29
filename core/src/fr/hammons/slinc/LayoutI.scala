package fr.hammons.slinc

import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.deriving.Mirror.ProductOf
import scala.reflect.ClassTag
import scala.quoted.*
import java.lang.invoke.MethodType
import scala.annotation.varargs
import modules.DescriptorModule
import container.*

class LayoutI(platformSpecific: LayoutI.PlatformSpecific)(using dm: DescriptorModule)

object LayoutI:
  trait PlatformSpecific:
    val intLayout: IntLayout
    val longLayout: LongLayout
    val floatLayout: FloatLayout
    val shortLayout: ShortLayout
    val doubleLayout: DoubleLayout
    val pointerLayout: PointerLayout
    val byteLayout: ByteLayout
    def toCarrierType(dataLayout: DataLayout): Class[?]
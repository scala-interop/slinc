package fr.hammons.slinc

import scala.compiletime.{summonInline, erasedValue, constValue}
import scala.deriving.Mirror.ProductOf
import scala.reflect.ClassTag
import scala.quoted.*
import java.lang.invoke.MethodType
import scala.annotation.varargs
import modules.DescriptorModule
import container.*

class LayoutI(platformSpecific: LayoutI.PlatformSpecific):
  extension (d: Descriptor)
    def toMethodType: MethodType =
      import platformSpecific.toCarrierType
      d match
        case Descriptor(head +: tail, vargs, None) =>
          VoidHelper
            .methodTypeV(
              toCarrierType(head),
              tail.concat(vargs).map(toCarrierType)*
            )
            .nn
        case Descriptor(head +: tail, vargs, Some(r)) =>
          MethodType
            .methodType(
              toCarrierType(r),
              toCarrierType(head),
              tail.concat(vargs).map(toCarrierType)*
            )
            .nn
        case Descriptor(_, _, None) => VoidHelper.methodTypeV().nn
        case Descriptor(_, _, Some(r)) =>
          MethodType.methodType(toCarrierType(r)).nn

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
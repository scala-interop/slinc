package fr.hammons.sffi

import scala.compiletime.summonInline
import java.lang.invoke.MethodHandle

trait WInTransition:
  self: WBasics & WLayoutInfo => 

  type Variadic = Container[LayoutInfo *::: InTransition *::: End]
    
  //opaque type NativeCompat = Object
  trait InTransition[A]:
    protected def toNativeCompat(obj: Object): Object = obj
    def to(a: A): Allocator ?=> Object

  object InTransition:
    def apply[A](using i: InTransition[A]) = i

  extension [A](a:A)(using i: InTransition[A])
    def to: Allocator ?=> Object = i.to(a)

  // implicit inline def toNativeCompat[A](a: A): Allocator ?=> NativeCompat = summonInline[InTransition[A]].to(a)

  given intInTransition: InTransition[Int]
  given byteInTransition: InTransition[Byte]
  given floatInTransition: InTransition[Float]
  given longInTransition: InTransition[Long]
  //given variadicInTransition: InTransition[Seq[Variadic]]
  given ptrInTransition[A]: InTransition[Pointer[A]]

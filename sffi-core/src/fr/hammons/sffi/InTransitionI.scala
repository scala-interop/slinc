package fr.hammons.sffi

trait InTransitionI[Allocator, LayoutInfo[A <: AnyKind] <: LayoutInfoI[?]#LayoutInfo[A]]: 
  type Variadic = Container[LayoutInfo *::: InTransition *::: End]

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
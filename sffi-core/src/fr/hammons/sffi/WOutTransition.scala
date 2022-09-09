package fr.hammons.sffi

trait WOutTransition:
  self: WBasics & WLayoutInfo =>

  trait OutTransition[A]:
    def from(o: Object): A


  given intOutTransition: OutTransition[Int]
  given floatOutTransition: OutTransition[Float]
  given longOutTransition: OutTransition[Long]
  given ptrOutTransition[A](using LayoutInfo[A]): OutTransition[Pointer[A]]

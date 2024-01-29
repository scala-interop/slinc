package fr.hammons.slinc.internal

//array can be part of a struct, implying that whenever something is parameter compatible, it's struct compatible
protected[slinc] trait StructCompatible[A]

object StructCompatible:
  given [A](using WhollyCompatible[A]): StructCompatible[A] with {}

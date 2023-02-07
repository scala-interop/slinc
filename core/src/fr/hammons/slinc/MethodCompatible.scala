package fr.hammons.slinc

trait MethodCompatible[A]

object MethodCompatible:
  given MethodCompatible[Byte] with {}
  given MethodCompatible[Short] with {}
  given MethodCompatible[Int] with {}
  given MethodCompatible[Long] with {}

  given MethodCompatible[Float] with {}
  given MethodCompatible[Double] with {}

  private val mthdCompatPtr = new MethodCompatible[Ptr[?]] {}
  given [A]: MethodCompatible[Ptr[A]] =
    mthdCompatPtr.asInstanceOf[MethodCompatible[Ptr[A]]]

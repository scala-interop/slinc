package fr.hammons.slinc

trait NativeOutCompatible[A]

object NativeOutCompatible:
  given NativeOutCompatible[Byte] with {}
  given NativeOutCompatible[Short] with {}
  given NativeOutCompatible[Int] with {}
  given NativeOutCompatible[Long] with {}
  given NativeOutCompatible[Char] with {}
  given NativeOutCompatible[Float] with {}
  given NativeOutCompatible[Double] with {} 
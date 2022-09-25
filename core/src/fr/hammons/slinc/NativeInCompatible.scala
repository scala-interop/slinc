package fr.hammons.slinc

trait NativeInCompatible[A]

object NativeInCompatible:
  given NativeInCompatible[Int] with {}
  given NativeInCompatible[Float] with {}
  given NativeInCompatible[Long] with {}
  given NativeInCompatible[Double] with {}
  given NativeInCompatible[Byte] with {}
  given NativeInCompatible[Short] with {}
  given NativeInCompatible[Char] with {}
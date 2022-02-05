package io.gitlab.mhammons.slinc.components

//todo: erased when that's not experimental
trait Initializer[T](using Integral[T]):
   def fromByte(b: Byte): T
   def fromShort(s: Short): Option[T]
   def fromShortOrFail(s: Short): T = fromShort(s).getOrElse(
     throw new ExceptionInInitializerError(
       s"Could not initialize the type from $s"
     )
   )
   def fromInt(i: Int): Option[T]
   def fromIntOrFail(i: Int): T = fromInt(i).getOrElse(
     throw new ExceptionInInitializerError(
       s"Could not initialize the type from $i"
     )
   )
   def fromLong(l: Long): Option[T]
   def fromLongOrFail(l: Long): T = fromLong(l).getOrElse(
     throw new ExceptionInInitializerError(
       s"Could not initialize the type from $l"
     )
   )

object Initializer:
   given Initializer[Byte] with
      def fromByte(b: Byte): Byte = b
      def fromShort(s: Short) = if s > Byte.MaxValue then None else Some(s.toByte)
      def fromInt(i: Int) = if i > Byte.MaxValue then None else Some(i.toByte)
      def fromLong(l: Long) = if l > Byte.MaxValue then None else Some(l.toByte)

   given Initializer[Short] with
      def fromByte(b: Byte) = b.toShort
      def fromShort(s: Short) = Some(s)
      def fromInt(i: Int) = None
      def fromLong(l: Long) = None

   given Initializer[Int] with
      def fromByte(b: Byte) = b.toInt
      def fromShort(s: Short) = Some(s.toInt)
      def fromInt(i: Int) = Some(i)
      def fromLong(l: Long) = None

   given Initializer[Long] with
      def fromByte(b: Byte) = b.toLong
      def fromShort(s: Short) = Some(s.toLong)
      def fromInt(i: Int) = Some(i.toLong)
      def fromLong(l: Long) = Some(l.toLong)

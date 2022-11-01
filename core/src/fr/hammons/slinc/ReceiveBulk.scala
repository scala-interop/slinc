package fr.hammons.slinc

import scala.reflect.ClassTag

trait ReceiveBulk[A]:
  def from(m: Mem, offset: Bytes, num: Int): IArray[A]

object ReceiveBulk:
  given [A](using r: Receive[A], l: LayoutOf[A])(using
      ClassTag[A]
  ): ReceiveBulk[A] with
    def from(m: Mem, offset: Bytes, num: Int): IArray[A] =
      var i = 0
      val array = Array.ofDim[A](num)
      while i < num do
        array(i) = r.from(m, offset + (l.layout.size * i))
        i += 1

      IArray.unsafeFromArray(array)

  given ReceiveBulk[Int] with
    def from(m: Mem, offset: Bytes, num: Int): IArray[Int] =
      IArray.unsafeFromArray(m.readIntArray(offset, num))

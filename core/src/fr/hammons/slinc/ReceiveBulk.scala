package fr.hammons.slinc

import scala.reflect.ClassTag
import fr.hammons.slinc.modules.DescriptorModule

trait ReceiveBulk[A]:
  def from(m: Mem, offset: Bytes, num: Int): IArray[A]

object ReceiveBulk:
  given [A](using
      r: Receive[A]
  )(using DescriptorOf[A], DescriptorModule, ClassTag[A]): ReceiveBulk[A] with
    def from(m: Mem, offset: Bytes, num: Int): IArray[A] =
      var i = 0
      val array = Array.ofDim[A](num)
      while i < num do
        array(i) = r.from(m, offset + (DescriptorOf[A].size * i))
        i += 1

      IArray.unsafeFromArray(array)

  given ReceiveBulk[Int] with
    def from(m: Mem, offset: Bytes, num: Int): IArray[Int] =
      IArray.unsafeFromArray(m.readIntArray(offset, num))

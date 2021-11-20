package io.gitlab.mhammons.slinc

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.annotation.tailrec

final case class PerfectHashtable[T](values: ArraySeq[T]):
   private val size = values.length
   def apply(str: String) = values(
     PerfectHashtable.firstHash(str, size)
   )

object PerfectHashtable:
   val intMask = 0xffffffffL

   inline def firstHash(string: String, length: Int) =
      math.abs(string.hashCode % length)
   @tailrec
   final def altStringHash(
       string: String,
       idx: Int,
       length: Int,
       result: Int = 31
   ): Int =
      if (idx < length)
         altStringHash(string, idx + 1, length, 31 * result + string(idx))
      else result

   def runtimeConstruct[T: ClassTag](strings: Seq[String], values: Seq[T]) =
      var continue = true
      var mask = strings.size
      while continue do
         val hashed = strings.map(s => firstHash(s, mask))
         continue = hashed.distinct != hashed
         if continue then mask += 1
      val vs = ArraySeq.unsafeWrapArray {
         val arr = Array.ofDim[T](mask)
         val vList = strings
            .map(str => firstHash(str, mask))
            .zip(values)

         println(vList.maxBy(_._1))

         vList.foreach((idx, v) => arr(idx.toInt) = v)

         arr
      }
      new PerfectHashtable[T](vs)

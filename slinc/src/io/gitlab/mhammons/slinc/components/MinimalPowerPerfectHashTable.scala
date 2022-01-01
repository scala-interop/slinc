package io.gitlab.mhammons.slinc.components

import scala.reflect.ClassTag
import scala.collection.immutable.ArraySeq

final case class MinimalFastPerfectHashtable[T](
    salts: Array[Int],
    values: Array[T]
):
   val mask = salts.size - 1
   def apply(k: String) = values(
     MinimalFastPerfectHashtable.indexOfStr(k, salts, mask)
   )

object MinimalFastPerfectHashtable:
   val powersOf2 = LazyList.iterate(1)(_ << 1)

   def runtimeConstruct[T: ClassTag](strings: Seq[String], values: Seq[T]) =
      val size = powersOf2
         .find(_ > strings.size)
         .getOrElse(
           throw new Exception("There were more strings than INT.maxInt")
         )
      val salts = findSalts(strings, size - 1).toArray
      val vs =
         strings
            .map(indexOfStr(_, salts, size - 1))
            .zip(values)
            .tapEach(println)
            .sortBy(_._1)
            .map(_._2)
            .toArray
      MinimalFastPerfectHashtable[T](salts, vs)

   final def indexOfStr(str: String, salts: Array[Int], mask: Int) =
      val salt = salts(secondHash(str, mask, 435))
      if salt < 0 then (salt * -1) - 1
      else secondHash(str, mask, salt)

   inline def secondHash(inline string: String, mask: Int, salt: Int) =
      var h = string.hashCode + salt
      h ^= (h >> 20) ^ (h >> 12)
      h ^= (h >> 7) ^ (h >> 4)
      h ^= (h >> 4)
      h & 0xfffffff & mask

   private def checkUniqueAndUsingAvailable(
       slotsClaimed: Array[Int],
       slotsAvailable: Array[Boolean],
       numSeen: Array[Boolean]
   ) =
      var zeroSeen = false
      var i = 0
      var last = 0
      var continue = true
      while i < slotsClaimed.length && continue do
         if slotsClaimed(i) != -1 then
            if !numSeen(
                 slotsClaimed(i)
               ) && slotsAvailable(
                 slotsClaimed(i)
               )
            then numSeen(slotsClaimed(i)) = true
            else continue = false
         i += 1
      continue

   private def findSaltsForGroup(
       available: Array[Boolean],
       group: Array[(String, Int)],
       mask: Int
   ): Int =
      val slotsClaimed = Array.fill[Int](available.length)(-1)
      if group.size == 1 then
         val spot = available.indexOf(true)
         available(spot) = false
         -(spot + 1)
      else
         var continue = true
         var last = -1

         val numSeen = Array.fill(available.length)(false)

         while continue do
            last += 1

            var i = 0

            while i < group.size do
               val (str, j) = group(i)
               slotsClaimed(j) = secondHash(str, mask, last)
               i += 1

            continue = !checkUniqueAndUsingAvailable(
              slotsClaimed,
              available,
              numSeen
            )
            i = 0
            while i < slotsClaimed.length do
               numSeen(i) = false
               i += 1
         var i = 0
         while i < slotsClaimed.size do
            val j = slotsClaimed(i)
            if j != -1 then available(j) = false
            i += 1
         last

   def findSalts(arr: Seq[String], mask: Int) =
      val finalSize = mask + 1
      val nArr =
         arr.zipWithIndex.map((str, idx) =>
            (secondHash(str, mask, 435), str, idx)
         )
      println(nArr)
      val sortByBucketsTaken =
         nArr
            .groupMap(_._1)(tup => (tup._2, tup._3))
            .toList
            .sortBy(_._2.size)
            .reverse

      val slotsLeft = Array.fill[Boolean](finalSize)(true)
      val res = sortByBucketsTaken.foldLeft(
        ArraySeq.unsafeWrapArray(Array.ofDim[Int](mask + 1))
      ) { (salts, thisGroup) =>
         println(s"slots left: ${slotsLeft.mkString}")
         println(s"this group: ${thisGroup}")
         val salt =
            findSaltsForGroup(slotsLeft, thisGroup._2.toArray, mask)

         println(s"new slots: ${slotsLeft.mkString}")
         salts.updated(thisGroup._1, salt)
      }

      res.toSeq

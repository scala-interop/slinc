package io.gitlab.mhammons.slinc.components
import scala.quoted.*
import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.collection.immutable.ArraySeq
import scala.compiletime.constValue
import scala.compiletime.ops.int.+

case class MinimalPerfectHashtable[T](
    val salts: Array[Int],
    val values: Array[T]
):
   val mask = values.length

   def apply(str: String) = values(
     MinimalPerfectHashtable.indexOfStr(str, salts, mask)
   )

object MinimalPerfectHashtable:
   val intMask = 0xffffffffL

   // def firstHash(string: String, mask: Int) =
   //    secondHash(string, mask, 435)
   // var h = string.hashCode
   // h ^= (h >> 20) ^ (h >> 12)
   // h ^= (h >> 7) ^ (h >> 4)
   // math.abs(h) % mask

   inline def secondHash(inline string: String, mask: Int, salt: Int) =
      var h = string.hashCode + salt
      // h ^= (h >> 20) ^ (h >> 12)
      // h ^= (h >> 7) ^ (h >> 4)
      h ^= (h >> 4)
      math.abs(h % mask)

   inline def apply[T](inline strings: String*)(values: T*) = ${
      applyImpl[T]('strings, 'values)
   }

   def findSalts(arr: Seq[String]) =
      val mask = arr.size
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

      def checkUniqueAndUsingAvailable(
          slotsClaimed: Array[Int],
          slotsAvailable: Array[Boolean],
          numSeen: Array[Boolean]
      ) = {
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
      }

      def findSalts(
          available: Array[Boolean],
          group: Array[(String, Int)],
          mask: Int
      ): Int =
         val slotsClaimed = Array.fill[Int](mask)(-1)
         if group.size == 1 then
            val spot = available.indexOf(true)
            available(spot) = false
            -(spot + 1)
         else
            var continue = true
            var last = -1

            val numSeen = Array.fill(slotsClaimed.length)(false)

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

      val slotsLeft = Array.fill[Boolean](arr.size)(true)
      val res = sortByBucketsTaken.foldLeft(
        ArraySeq.unsafeWrapArray(Array.ofDim[Int](arr.size))
      ) { (salts, thisGroup) =>
         println(s"slots left: ${slotsLeft.mkString}")
         println(s"this group: ${thisGroup}")
         val salt =
            findSalts(slotsLeft, thisGroup._2.toArray, slotsLeft.size)

         println(s"new slots: ${slotsLeft.mkString}")
         salts.updated(thisGroup._1, salt)
      }

      res.toSeq

   def applyImpl[T: Type](strings: Expr[Seq[String]], values: Expr[Seq[T]])(
       using Quotes
   ) =
      import quotes.reflect.*
      val reifiedStrings = strings.valueOrAbort
      val stringSize = reifiedStrings.size

      val singletonSize = Singleton(Literal(IntConstant(stringSize))).tpe.asType
      val tClassTag = Expr.summon[ClassTag[T]].getOrElse(???)

      val salts = compilerCache.get(reifiedStrings).getOrElse {
         val salts = ArraySeq.from(findSalts(reifiedStrings))
         compilerCache += reifiedStrings -> salts
         salts
      }
      val slots = Expr(salts)

      singletonSize match
         case '[i] =>
            '{
               val size = ${ Expr(stringSize) }
               val salts = Array($slots*)
               val vs =
                  $strings
                     .map(indexOfStr(_, salts, size))
                     .zip($values)
                     .sortBy(_._1)
                     .map(_._2)
                     .toArray(using $tClassTag)
               new MinimalPerfectHashtable[T](
                 salts,
                 vs
               )
            }

   var compilerCache = Map.empty[Seq[String], Seq[Int]]

   def runtimeConstruct[T: ClassTag](strings: Seq[String], values: Seq[T]) =
      val mask = strings.size
      val salts = findSalts(strings).toArray
      val vs =
         strings
            .map(indexOfStr(_, salts, mask))
            .zip(values)
            .tapEach(println)
            .sortBy(_._1)
            .map(_._2)
            .toArray

      new MinimalPerfectHashtable[T](salts, vs)

   def isNegative(x: Int) =
      -(x >> 31)

   def isPositive(x: Int) =
      -(-x >> 31)

   def bitwiseAbs(x: Int) =
      (isPositive(x) * x) + (isNegative(x) * -x)

   final def indexOfStr(str: String, salts: Array[Int], mask: Int) =
      val salt = salts(secondHash(str, mask, 435))
      if salt < 0 then (salt * -1) - 1
      else secondHash(str, mask, salt)

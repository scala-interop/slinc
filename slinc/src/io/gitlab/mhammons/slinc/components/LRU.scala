package io.gitlab.mhammons.slinc.components

class LRU(size: Int):
   val keys: Array[String] = Array.ofDim[String](size)
   val values: Array[Any] = Array.ofDim[Any](size)
   var used = 0
   val almost = math.max(size - 1, 0)

   inline def get[A](signature: String, inline a: A) =
      val idx = keys.indexOf(signature)

      if idx == 0 then values(0).asInstanceOf[A]
      else if idx > -1 then
         val result = values(idx).asInstanceOf[A]
         System.arraycopy(keys, 0, keys, 1, idx)
         System.arraycopy(values, 0, values, 1, idx)
         values(0) = result
         keys(0) = signature
         result
      else
         val result = a
         System.arraycopy(keys, 0, keys, 1, almost)
         System.arraycopy(values, 0, values, 1, almost)
         values(0) = result
         keys(0) = signature
         used += (if used == size then 0 else 1)
         result

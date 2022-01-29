package io.gitlab.mhammons.slinc.components

class HMap[T <: Tuple](arr: Array[Any]):
   def get[K <: Singleton & Int](k: K): HMap.GetMatch[T, K] =
      arr(k).asInstanceOf[HMap.GetMatch[T, K]]

object HMap:
   type GetMatch[Tup <: Tuple, K] = Tup match
      case (K *: t *: EmptyTuple) *: ? => t
      case ? *: rest                   => GetMatch[rest, K]

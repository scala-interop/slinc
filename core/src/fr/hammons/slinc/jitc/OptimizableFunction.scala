package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicInteger
import scala.compiletime.codeOf

sealed trait OptimizableFunction[F]:
  def apply[O](fn: F => O): O

class UnoptimizedFunction[F](
    aotcFn: F,
    val originalCode: String,
    limit: Int,
    updateFn: () => Unit
) extends OptimizableFunction[F]:
  val counter: AtomicInteger = AtomicInteger(0)

  def getCount() = counter.getOpaque()

  def apply[O](fn: F => O): O =
    val res = fn(aotcFn)
    val currentCount = counter.getOpaque()
    if currentCount + 1 >= limit then
      counter.setOpaque(currentCount + 1)
      updateFn()
      res
    else
      counter.setOpaque(currentCount + 1)
      res

object UnoptimizedFunction:
  inline def apply[F](
      inline aotc: F,
      limit: Int,
      updateFn: () => Unit
  ): UnoptimizedFunction[F] =
    new UnoptimizedFunction[F](aotc, codeOf(aotc), limit, updateFn)

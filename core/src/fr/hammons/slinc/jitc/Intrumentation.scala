package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicInteger
import fr.hammons.slinc.fnutils.Fn

trait Instrumentation:
  def getCount(): Int
  protected def toInstrumented[A](a: A): Instrumented[A] = a
  opaque type Instrumented[A] = A
  opaque type InstrumentedFn[A] <: A = A

  def instrument[A](a: A): Instrumented[A]

  def apply[A, B <: Tuple, C, D, E](fn: A)(using
      Fn[A, B, C],
      C =:= Instrumented[D],
      Fn[E, B, D]
  ): InstrumentedFn[E] =
    fn.asInstanceOf[E]

class CountbasedInstrumentation extends Instrumentation:
  private val count = AtomicInteger(0)
  def getCount() = count.get()
  private def incrementCount(): Int =
    var succeeded = false
    var res = 0
    while !succeeded do
      res = count.get()
      succeeded = count.compareAndSet(res, res + 1)

    res + 1

  def instrument[A](a: A): Instrumented[A] =
    incrementCount()
    toInstrumented(a)

object IgnoreInstrumentation extends Instrumentation:
  def getCount() = 0
  def instrument[A](a: A): Instrumented[A] = toInstrumented(a)

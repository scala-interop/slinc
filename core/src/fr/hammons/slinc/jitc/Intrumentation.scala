package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicInteger
import fr.hammons.slinc.fnutils.Fn
import scala.annotation.implicitNotFound

trait Instrumentation:
  def getCount(): Int
  protected def toInstrumented[A](a: A): Instrumented[A] = a
  opaque type Instrumented[A] = A
  opaque type InstrumentedFn[A] <: A = A

  def instrument[A](a: A): Instrumented[A]

  def apply[A, B <: Tuple, C, D, E](fn: A)(using
      @implicitNotFound(
        "Could not find Fn[${A}, ${B}, Instrumented[${C}]"
      ) ev1: Fn[A, B, Instrumented[C]],
      ev2: Fn[E, B, C]
  ): InstrumentedFn[E] =
    fn.asInstanceOf[E]

class CountbasedInstrumentation(triggerFn: () => Unit, triggerLimit: Int)
    extends Instrumentation:
  private val count = AtomicInteger(0)
  final def getCount() = count.getAcquire()
  private def incrementCount(): Unit =
    var succeeded = false
    var res = 0
    while !succeeded do
      res = count.get()
      succeeded = count.compareAndSet(res, res + 1)

    res += 1
    if res >= triggerLimit then triggerFn()

  def instrument[A](a: A): Instrumented[A] =
    incrementCount()
    toInstrumented(a)

object IgnoreInstrumentation extends Instrumentation:
  def getCount() = 0
  def instrument[A](a: A): Instrumented[A] = toInstrumented(a)

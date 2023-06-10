package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicInteger
import fr.hammons.slinc.fnutils.Fn
import scala.annotation.implicitNotFound
import fr.hammons.slinc.jitc.OptimizableFn.limitSetting

trait Instrumentation:
  def getCount(): Int
  opaque type Instrumented[A] = A
  opaque type InstrumentedFn[A] <: A = A

  protected def bootInstrumentation(): Unit
  protected def finishInstrumentation(): Unit
  inline def instrument[A](inline a: A): Instrumented[A] =
    bootInstrumentation()
    val ret = a
    finishInstrumentation()
    ret

  def apply[A, B <: Tuple, C, D, E](fn: A)(using
      @implicitNotFound(
        "Could not find Fn[${A}, ${B}, Instrumented[${C}]"
      ) ev1: Fn[A, B, Instrumented[C]],
      ev2: Fn[E, B, C]
  ): InstrumentedFn[E] =
    fn.asInstanceOf[E]

  def shouldOpt: Boolean

class CountbasedInstrumentation(triggerLimit: Int) extends Instrumentation:
  private val count = AtomicInteger(0)
  final def getCount() = count.get()
  private def incrementCount(): Unit =
    count.incrementAndGet()

  protected def bootInstrumentation(): Unit = incrementCount()
  protected def finishInstrumentation(): Unit = ()

  def shouldOpt: Boolean = count.getOpaque() >= triggerLimit

case class IgnoreInstrumentation(optimize: Boolean) extends Instrumentation:
  def getCount() = 0
  protected def bootInstrumentation(): Unit = ()
  protected def finishInstrumentation(): Unit = ()

  def shouldOpt: Boolean = optimize

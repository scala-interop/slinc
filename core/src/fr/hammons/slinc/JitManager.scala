package fr.hammons.slinc

import Fn.andThen
import scala.compiletime.uninitialized
import java.util.concurrent.atomic.AtomicReference
import scala.quoted.staging.*
import java.util.concurrent.Executors
import scala.concurrent.Promise
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext
import scala.quoted.{Quotes, Expr}
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Failure.apply
import scala.util.Failure
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicInteger
import scala.reflect.ClassTag

type JitCompiler = [A] => ((Quotes) ?=> Expr[A]) => A
object DummyManager extends JitManager:
  def jitc[F, Input <: Tuple, Output](
      atomicRef: F => Unit,
      lowSpeed: F,
      highSpeed: JitCompiler => F
  )(using f: Fn[F, Input, Output]): Unit = atomicRef(lowSpeed)

  def jitNow(): Unit = ()

trait JitManager:
  def jitc[F, Input <: Tuple, Output](
      atomicRef: F => Unit,
      lowSpeed: F,
      highSpeed: JitCompiler => F
  )(using f: Fn[F, Input, Output]): Unit

  def jitNow(): Unit

class JitManagerImpl(
    compiler: Compiler,
    jitThreshold: Int = 1000,
    checkRate: FiniteDuration = 100.millis
) extends JitManager:
  private val shutDown = AtomicBoolean(false)
  private val jitAllNow = AtomicBoolean(false)
  given Compiler = compiler
  private val tp = Executors.newWorkStealingPool(1).nn
  private val runnable: Runnable = () => {
    shutDown.set(true)
    tp.shutdown()
  }
  Runtime.getRuntime().nn.addShutdownHook(Thread(runnable))
  given ExecutionContext = ExecutionContext.fromExecutor(tp)

  private case class StuffToJit[A](
      ref: A => Unit,
      counter: AtomicInteger,
      fnGen: () => A
  )
  private val toWatch = AtomicReference(
    Vector.empty[StuffToJit[?]]
  )

  Future {
    val startTime = System.currentTimeMillis()
    var nextCycleEnd = startTime + checkRate.toMillis
    while !shutDown.get() do
      import language.unsafeNulls
      val windowsToWatch = toWatch.get()
      val toJit =
        if jitAllNow.get then windowsToWatch
        else
          windowsToWatch
            .filter(jitTarget => jitTarget.counter.get() > jitThreshold)
      toJit.foreach(jitTarget =>
        val code = jitTarget.fnGen()
        jitTarget.ref(code)
        jitTarget.counter.set(Int.MinValue)
      )
      jitAllNow.compareAndSet(true, false)

      var succeeded = false
      while !succeeded do
        val windowsToRemove = toWatch.get()
        succeeded = toWatch.compareAndSet(
          windowsToRemove,
          windowsToRemove.filter(jitTarget => jitTarget.counter.get() >= 0)
        )

      val lastCycleEnd = nextCycleEnd
      nextCycleEnd += checkRate.toMillis
      while nextCycleEnd <= System.currentTimeMillis() do
        nextCycleEnd += checkRate.toMillis
      Thread.sleep(Math.max(0, lastCycleEnd - System.currentTimeMillis()))

    println("shutdown")
  }.onComplete {
    case Failure(t) => System.err.nn.println(t.getMessage())
    case _          => ()
  }

  def jitc[F, Input <: Tuple, Output](
      atomicRef: F => Unit,
      lowSpeed: F,
      highSpeed: JitCompiler => F
  )(using f: Fn[F, Input, Output]): Unit =
    val counter = AtomicInteger(0)
    atomicRef(
      lowSpeed.andThen { z =>
        counter.getAndIncrement()
        z
      }
    )
    var succeeded = false
    while !succeeded do
      import language.unsafeNulls
      val watchList = toWatch.get()
      succeeded = toWatch.compareAndSet(
        watchList,
        watchList :+ StuffToJit(
          atomicRef,
          counter,
          () => highSpeed([A] => (fn: ((Quotes) ?=> Expr[A])) => run[A](fn))
        )
      )

  def jitNow(): Unit =
    jitAllNow.compareAndSet(false, true)
    while jitAllNow.get() do Thread.sleep(150)
//def jitc2[F](atomicRef: AtomicReference[F])

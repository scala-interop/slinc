package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicReference
import java.util.UUID
import java.util.concurrent.atomic.AtomicStampedReference
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.switch
import scala.compiletime.uninitialized

sealed trait OptimizableFn[F, G]:
  final val uuid = UUID.randomUUID().nn

  def isOptimized: Boolean

  def forceOptimize(using G): F
  def triggerOptimization(using G): Unit
  def get(using G): F

final class FnToJit[F, G](
    optimizer: JitCService,
    inst: Instrumentation,
    optimized: G ?=> JitCompiler => F,
    f: G ?=> (i: Instrumentation) => i.InstrumentedFn[F]
) extends OptimizableFn[F, G]:
  import scala.language.unsafeNulls
  private var state: Int = 0
  private val _optFn: AtomicReference[F] = AtomicReference()
  private var fn: F = uninitialized
  private var fastFn: F = uninitialized

  def isOptimized: Boolean = state == 3

  final def forceOptimize(using G): F =
    triggerOptimization
    while fastFn == null do fastFn = _optFn.getOpaque()
    state = 3
    fastFn

  final def triggerOptimization(using G): Unit =
    optimizer.jitC(
      uuid,
      jitCompiler =>
        val opt = optimized(jitCompiler)
        _optFn.setOpaque(
          opt
        )
    )

  final def get(using G): F =
    (state: @switch) match
      case 0 =>
        fn = f(inst)
        state = 1
        get
      case 1 =>
        if inst.shouldOpt then
          state = 2
          get
        fn
      case 2 =>
        if _optFn.getOpaque() == null then triggerOptimization

        state = 3
        get

      case 3 =>
        fastFn = _optFn.getOpaque()
        if fastFn != null then
          state = 4
          get
        else fn
      case 4 => fastFn
      case _ => fn

final class NeverJitFn[F, G](
    f: G ?=> (i: Instrumentation) => i.InstrumentedFn[F]
) extends OptimizableFn[F, G]:
  private var _f: F | Null = null
  def isOptimized: Boolean = false
  final def forceOptimize(using G): F =
    _f = f(IgnoreInstrumentation(false))
    _f.nn

  final def triggerOptimization(using G): Unit = ()

  final def get(using G): F = forceOptimize

final class InstantJitFn[F, G](
    optimizer: JitCService,
    f: G ?=> JitCompiler => F
) extends OptimizableFn[F, G]:
  def isOptimized: Boolean = _permFn != null
  private val _fn: AtomicReference[F] = AtomicReference()
  private var _permFn: F | Null = null

  final def forceOptimize(using G): F =
    val fn = _fn.getOpaque()
    if fn == null then
      triggerOptimization
      while _fn.getOpaque() == null do {}
      _fn.getOpaque().nn
    else fn

  final def get(using G): F =
    if _permFn != null then _permFn.nn
    else
      _permFn = forceOptimize
      _permFn.nn

  final def triggerOptimization(using G): Unit =
    optimizer.jitC(
      uuid,
      jitCompiler =>
        val optimized = f(jitCompiler)
        _fn.setOpaque(optimized)
    )

object OptimizableFn:
  val modeSetting = "slinc.jitc.mode"
  val limitSetting = "slinc.jitc.jit-limit"
  def apply[F, G](optimized: G ?=> JitCompiler => F)(
      unoptimizedFn: G ?=> (i: Instrumentation) => i.InstrumentedFn[F]
  ) =
    val mode = sys.props.getOrElseUpdate("slinc.jitc.mode", "standard")
    mode match
      case "standard" =>
        val limit =
          sys.props.getOrElseUpdate("slinc.jitc.jit-limit", "10000").toIntOption
        limit match
          case None => throw Error("slinc.jitc.jit-limit should be an integer")
          case Some(value) =>
            new FnToJit[F, G](
              JitCService.standard,
              CountbasedInstrumentation(value),
              optimized,
              unoptimizedFn
            )

      case "never" | "disabled" =>
        new NeverJitFn[F, G](
          unoptimizedFn
        )

      case "immediate" =>
        new InstantJitFn[F, G](JitCService.synchronous, optimized)

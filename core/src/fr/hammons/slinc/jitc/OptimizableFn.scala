package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

sealed trait OptimizableFn[F, G]:
  final val uuid = UUID.randomUUID().nn

  def forceOptimize(using G): F
  def triggerOptimization(using G): Unit
  def get(using G): F

final class FnToJit[F, G](
    optimizer: JitCService,
    inst: (() => Unit) => Instrumentation,
    optimized: G ?=> JitCompiler => F,
    f: G ?=> (i: Instrumentation) => i.InstrumentedFn[F]
) extends OptimizableFn[F, G]:
  private val _fn: AtomicReference[F] = AtomicReference()
  private val _optFn: AtomicReference[F] = AtomicReference()
  private var _permOptFn: F | Null = null

  final def forceOptimize(using G): F =
    triggerOptimization
    while _optFn.getOpaque() == null do {}
    _permOptFn = _optFn.getOpaque().nn
    _permOptFn.nn

  def triggerOptimization(using G): Unit =
    optimizer.jitC(
      uuid,
      jitCompiler =>
        val opt = optimized(jitCompiler)
        _optFn.setOpaque(
          opt
        )
    )

  private def tryGetOptFn(using G) =
    val optFn = _optFn.getOpaque()

    if optFn != null then
      _permOptFn = optFn
      optFn
    else getFn

  private def getFn(using G) =
    val fn = _fn.getOpaque()
    if fn != null then fn
    else
      val nFn = f(inst(() => triggerOptimization))
      _fn.set(nFn)
      nFn

  final def get(using G): F =
    if _permOptFn != null then _permOptFn.nn
    else tryGetOptFn

final class NeverJitFn[F, G](
    f: G ?=> (i: Instrumentation) => i.InstrumentedFn[F]
) extends OptimizableFn[F, G]:
  private var _f: F | Null = null
  final def forceOptimize(using G): F =
    _f = f(IgnoreInstrumentation)
    _f.nn

  final def triggerOptimization(using G): Unit = ()

  final def get(using G): F = forceOptimize

final class InstantJitFn[F, G](
    optimizer: JitCService,
    f: G ?=> JitCompiler => F
) extends OptimizableFn[F, G]:
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

// final class OptimizableFn[F, G](
//     optimizer: JitCService,
//     inst: Instrumentation = new CountbasedInstrumentation
// )(
//     f: G ?=> (i: Instrumentation) => i.InstrumentedFn[F],
//     limit: Int
// )(optimized: G ?=> JitCompiler => F):
//   private val _fn: AtomicReference[F] = AtomicReference()
//   final val uuid = UUID.randomUUID().nn
//   private val _optFn: AtomicReference[F] = AtomicReference()
//   private var _permOptFn: F | Null = null

//   final def forceOptimize(using G) =
//     optimizer.jitC(
//       uuid,
//       jitCompiler =>
//         val opt = optimized(jitCompiler)
//         _optFn.setOpaque(
//           opt
//         )
//     )
//     while _optFn.getOpaque() == null do {}
//     _permOptFn = _optFn.getOpaque().nn
//     _permOptFn.nn

//   private def optimize(using G): F =
//     optimizer.jitC(
//       uuid,
//       jitCompiler =>
//         val opt = optimized(jitCompiler)
//         _optFn.setOpaque(opt)
//     )
//     if optimizer.async then getFn
//     else
//       while _optFn.getOpaque() == null do {}
//       _optFn.getOpaque().nn

//   private def checkOptTrigger(using G) =
//     if inst.getCount() >= limit then optimize
//     else getFn

//   private def tryGetOptFn(using G) =
//     val optFn = _optFn.getOpaque()

//     if optFn != null then
//       _permOptFn = optFn
//       optFn
//     else checkOptTrigger

//   private def getFn(using G) =
//     val fn = _fn.getOpaque()
//     if fn != null then fn
//     else
//       val nFn = f(inst)
//       _fn.set(nFn)
//       nFn

//   final def get(using G): F =
//     if _permOptFn != null then _permOptFn.nn
//     else tryGetOptFn
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
              updateFn => CountbasedInstrumentation(updateFn, value),
              optimized,
              unoptimizedFn
            )

      case "never" | "disabled" =>
        new NeverJitFn[F, G](
          unoptimizedFn
        )

      case "immediate" =>
        new InstantJitFn[F, G](JitCService.synchronous, optimized)

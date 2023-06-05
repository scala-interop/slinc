package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

class OptimizableFn[F](
    optimizer: JitCService,
    inst: Instrumentation = new CountbasedInstrumentation
)(
    f: (i: Instrumentation) => i.InstrumentedFn[F],
    limit: Int
)(optimized: JitCompiler => F):
  private val _fn: AtomicReference[F] = AtomicReference(f(inst))
  val uuid = UUID.randomUUID().nn
  private val _optFn: AtomicReference[F] =
    if inst.getCount() >= limit then
      var opt: F | Null = null
      optimizer.jitC(uuid, jitCompiler => opt = optimized(jitCompiler))
      AtomicReference(opt.nn)
    else AtomicReference()

  def get: F =
    val optFn = _optFn.getOpaque()
    if optFn != null then optFn
    else
      if inst.getCount() >= limit then
        optimizer.jitC(
          uuid,
          jitCompiler =>
            val opt = optimized(jitCompiler)
            _optFn.setOpaque(
              opt
            )
        )
      _fn.getOpaque().nn

object OptimizableFn:
  val modeSetting = "slinc.jitc.mode"
  val limitSetting = "slinc.jitc.jit-limit"
  def apply[F](optimized: JitCompiler => F)(
      unoptimizedFn: (i: Instrumentation) => i.InstrumentedFn[F]
  ) =
    val mode = sys.props.getOrElseUpdate("slinc.jitc.mode", "standard")
    mode match
      case "standard" =>
        val limit =
          sys.props.getOrElseUpdate("slinc.jitc.jit-limit", "10000").toIntOption
        limit match
          case None => throw Error("slinc.jitc.jit-limit should be an integer")
          case Some(value) =>
            new OptimizableFn[F](
              JitCService.standard,
              CountbasedInstrumentation()
            )(unoptimizedFn, value)(optimized)

      case "never" | "disabled" =>
        new OptimizableFn[F](JitCService.synchronous, IgnoreInstrumentation)(
          unoptimizedFn,
          1
        )(optimized)

      case "immediate" =>
        new OptimizableFn[F](JitCService.synchronous, IgnoreInstrumentation)(
          unoptimizedFn,
          0
        )(optimized)

  def standard[F](
      optimized: JitCompiler => F
  )(unoptimizedFn: (i: Instrumentation) => i.InstrumentedFn[F], limit: Int) =
    new OptimizableFn[F](JitCService.standard)(unoptimizedFn, limit)(optimized)

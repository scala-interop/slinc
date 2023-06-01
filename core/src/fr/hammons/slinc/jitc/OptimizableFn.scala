package fr.hammons.slinc.jitc

import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

class OptimizableFn[F, G](
    optimizer: JitCService,
    inst: Instrumentation = new CountbasedInstrumentation
)(
    f: G ?=> (i: Instrumentation) => i.InstrumentedFn[F],
    limit: Int
)(optimized: G ?=> JitCompiler => F):
  private val _fn: AtomicReference[F] = AtomicReference()
  val uuid = UUID.randomUUID().nn
  private val _optFn: AtomicReference[F] = AtomicReference()
  private var _permOptFn: F | Null = null

  def forceOptimize(using G) = 
    optimizer.jitC(
      uuid, 
      jitCompiler => 
        val opt = optimized(jitCompiler)
        _optFn.setOpaque(
          opt
        )
    )
    while _optFn.getOpaque() == null do {}
    _permOptFn = _optFn.getOpaque().nn
    _permOptFn.nn


  def get(using G): F =
    if _permOptFn != null then 
      _permOptFn.nn
    else 
      val optFn = _optFn.getOpaque()

      if optFn != null then 
        _permOptFn = optFn
        optFn
      else 
        var fn = _fn.getOpaque()
        if fn == null then
          fn = f(inst)
          _fn.set(fn)

        if inst.getCount() >= limit then
          optimizer.jitC(
            uuid,
            jitCompiler =>
              val opt = optimized(jitCompiler)
              _optFn.setOpaque(
                opt
              )
          )
          if optimizer.async then fn.nn
          else
            while _optFn.getOpaque() == null do {}
            _optFn.getOpaque().nn
        else fn.nn

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
            new OptimizableFn[F, G](
              JitCService.standard,
              CountbasedInstrumentation()
            )(unoptimizedFn, value)(optimized)

      case "never" | "disabled" =>
        new OptimizableFn[F, G](JitCService.synchronous, IgnoreInstrumentation)(
          unoptimizedFn,
          1
        )(optimized)

      case "immediate" =>
        new OptimizableFn[F, G](JitCService.synchronous, IgnoreInstrumentation)(
          unoptimizedFn,
          0
        )(optimized)

package fr.hammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import fr.hammons.slinc.jitc.OptimizableFn
import fr.hammons.slinc.jitc.InstantJitFn
import fr.hammons.slinc.jitc.JitCService
import fr.hammons.slinc.jitc.FnToJit
import fr.hammons.slinc.jitc.CountbasedInstrumentation
import fr.hammons.slinc.jitc.IgnoreInstrumentation
import fr.hammons.slinc.jitc.Instrumentation

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.SampleTime))
class JitBenchmark:
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  def value: Int = 5

  val baseFn = (i: Int) => i + 5
  val iiFn =
    val ignoreInstrumentation: Instrumentation = IgnoreInstrumentation(false)
    ignoreInstrumentation((i: Int) => ignoreInstrumentation.instrument(i + 5))
  val ciFn =
    val countInstrumentation: Instrumentation = CountbasedInstrumentation(10000)
    countInstrumentation((i: Int) => countInstrumentation.instrument(i + 5))

  val instantFn: OptimizableFn[Int => Int, DummyImplicit] =
    InstantJitFn(JitCService.standard, jitc => jitc('{ (i: Int) => i + 5 }))

  val bareInstantFn = instantFn.get

  val jittedFn: FnToJit[Int => Int, DummyImplicit] = FnToJit(
    JitCService.standard,
    CountbasedInstrumentation(10000),
    jitc => jitc('{ (i: Int) => i + 5 }),
    inst => inst((i: Int) => inst.instrument(i + 5))
  )

  @Benchmark
  def base(b: Blackhole) =
    b.consume(baseFn(value))

  @Benchmark
  def ignoreInstrumented(b: Blackhole) =
    b.consume(iiFn(value))

  @Benchmark
  def countInstrumented(b: Blackhole) =
    b.consume(ciFn(value))

  @Benchmark
  def instant(b: Blackhole) =
    b.consume(instantFn.get(value))

  // @Benchmark
  // def bareInstant(b: Blackhole) =
  //   b.consume(bareInstantFn(value))

  @Benchmark
  def jitted(b: Blackhole) =
    b.consume(jittedFn.get(value))

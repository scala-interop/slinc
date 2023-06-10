package fr.hammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.atomic.AtomicReference
import fr.hammons.slinc.jitc.JitCService
import fr.hammons.slinc.jitc.JitCompiler
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.uninitialized
import java.util.UUID
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
class JitCompilerState:
  val fnRef: AtomicReference[Int => Int] = AtomicReference()

  val jitcAsync = JitCService.standard
  var uuid: UUID = uninitialized

  val methodToCompile: JitCompiler => Unit = jitc =>
    fnRef.setOpaque(jitc('{ (i: Int) => i }))

  @Setup(Level.Invocation)
  def setup(): Unit =
    uuid = UUID.randomUUID().nn
    fnRef.set(null)

@State(Scope.Thread)
class DummyState:
  val dummyFnToCompile: JitCompiler => Unit = jitc => jitc('{ (i: Int) => i })

  val dummyIds: Array[UUID] = Array.ofDim(100)

  @Setup(Level.Invocation)
  def setup(): Unit =
    for i <- 0 until dummyIds.size do dummyIds(i) = UUID.randomUUID().nn

@BenchmarkMode(Array(Mode.SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class JitCompilerBenchmark:

  @Benchmark
  def compilationSpeed(b: Blackhole, jcs: JitCompilerState) =
    b.consume:
      jcs.jitcAsync.jitC(jcs.uuid, jcs.methodToCompile)
      while jcs.fnRef.get() == null do {}

  @Benchmark
  @OperationsPerInvocation(101)
  def compileStress(b: Blackhole, jcs: JitCompilerState, ds: DummyState) =
    b.consume:
      for id <- ds.dummyIds do jcs.jitcAsync.jitC(id, ds.dummyFnToCompile)
      jcs.jitcAsync.jitC(jcs.uuid, jcs.methodToCompile)
      while jcs.fnRef.get() == null do {}

package fr.hammons.slinc.experimental

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import fr.hammons.slinc.Runtime
import fr.hammons.slinc.Platform
import fr.hammons.slinc.experimental.CLong
import org.openjdk.jmh.infra.Blackhole

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
class PlatformDependentTypesBench {
  given Runtime = new Runtime:
    def platform: Platform = Platform.LinuxX64

  val clong1 = CLong(1)
  val clong2 = CLong(2)

  val long1 = 1L
  val long2 = 2L

  def add[A <: Matchable](a: A, b: A)(using num: Integral[A]) = num.plus(a, b)

  @Benchmark
  def clongAdd(blackhole: Blackhole) =
    blackhole.consume(clong1 + clong2)

  @Benchmark
  def longAdd(blackhole: Blackhole) =
    blackhole.consume(add(long1, long2))
}

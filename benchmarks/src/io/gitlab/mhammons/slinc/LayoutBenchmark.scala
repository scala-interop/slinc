package io.gitlab.mhammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules",
    "jdk.incubator.foreign",
    "--enable-native-access",
    "ALL-UNNAMED"
  )
)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.SampleTime))
class LayoutBenchmark:

   val nativeCache = NativeCacheDefaultImpl()
   import Member.{int, float}
   type div_a = Struct {
      val a: float
      val b: float
   }
   type div_t = Struct {
      val quot: int
      val rem: int
      val div_a: div_a
   }

   type div_h = Struct {
      val o: int
      val r: int
      val div_t: div_t
   }

   @Benchmark
   def layoutNativeCache =
      nativeCache.layout[div_h].underlying.byteSize

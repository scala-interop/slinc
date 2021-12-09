package io.gitlab.mhammons.slinc_benches

import scala.util.Random
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import java.util.Arrays
import scala.annotation.tailrec
import io.gitlab.mhammons.slinc.*
import jdk.incubator.foreign.{SegmentAllocator, ResourceScope}

@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules",
    "jdk.incubator.foreign",
    "--enable-native-access",
    "ALL-UNNAMED"
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Array(Mode.SampleTime))
class StructBenchmark:
   case class div_t(a: Int, b: Int) derives Struct
   // case class div_nested(a: div_t, b: div_t) derives Struct

   @Param(Array("1", "100", "10000"))
   var reps: Int = _

   var rs: ResourceScope = _
   var segAlloc: SegmentAllocator = _
   // var divNested: Ptr[div_nested] = _
   var divRo: div_t = _

   var divSimple: Ptr[div_t] = _

   // var divN: div_nested = _

   @Setup(Level.Iteration)
   def setup() =
      rs = ResourceScope.newConfinedScope
      segAlloc = SegmentAllocator.arenaAllocator(rs)
      given SegmentAllocator = segAlloc
      divRo = div_t(5, 3)
      // divN = div_nested(divRo, divRo)
      divSimple = divRo.serialize
   // divNested = divN.serialize

   @TearDown(Level.Iteration)
   def teardown() =
      rs.close

   @Benchmark
   def allocate2Simple =
      given SegmentAllocator = segAlloc
      repeat(divRo.serialize, reps)

   // @Benchmark
   // def allocate2Nested =
   //    given SegmentAllocator = segAlloc
   //    repeat(divN.serialize, reps)

   @Benchmark
   def access2Simple =
      repeat(!divSimple.partial.a, reps)
   // @Benchmark
   // def access2Nested =
   //    repeat(!divNested.partial.a, reps)

   @Benchmark
   def accessROSimple =
      repeat(divRo.a, reps)

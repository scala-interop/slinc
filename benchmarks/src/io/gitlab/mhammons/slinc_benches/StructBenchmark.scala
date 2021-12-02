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
   import components.Member.int
   type div_t = Struct {
      val a: int
      val b: int
   }
   type div_nested = Struct {
      val a: div_t
      val b: div_t
   }

   type div_t_ro = Struct {
      val a: Int
      val b: Int
   }

   @Param(Array("1", "100", "10000"))
   var reps: Int = _

   var rs: ResourceScope = _
   var segAlloc: SegmentAllocator = _
   var divSimple: div_t = _
   var divNested: div_nested = _
   var div2Simple: div_t = _
   var div2Nested: div_nested = _
   var divRo: div_t_ro = _

   @Setup(Level.Iteration)
   def setup() =
      rs = ResourceScope.newConfinedScope
      segAlloc = SegmentAllocator.arenaAllocator(rs)
      given SegmentAllocator = segAlloc
      div2Simple = allocate[div_t]
      div2Nested = allocate[div_nested]
      divRo = allocate[div_t_ro]

   @TearDown(Level.Iteration)
   def teardown() =
      rs.close

   @Benchmark
   def allocate2Simple =
      given SegmentAllocator = segAlloc
      repeatInl(allocate[div_t], reps)

   @Benchmark
   def allocate2Nested =
      given SegmentAllocator = segAlloc
      repeatInl(allocate[div_nested], reps)

   @Benchmark
   def access2Simple =
      repeatInl(div2Simple.a(), reps)
   @Benchmark
   def access2Nested =
      repeatInl(div2Nested.a.a(), reps)

   @Benchmark
   def accessROSimple =
      repeatInl(divRo.a, reps)





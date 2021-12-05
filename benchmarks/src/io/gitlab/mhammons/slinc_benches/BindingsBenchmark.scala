package io.gitlab.mhammons.slinc_benches

import org.openjdk.jmh.annotations.*
import io.gitlab.mhammons.slinc.*
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import org.openjdk.jmh.annotations.Mode
import jnr.ffi.LibraryLoader
import io.gitlab.mhammons.polymorphics.MethodHandleHandler
import jdk.incubator.foreign.{SegmentAllocator, ResourceScope}
import components.NPtr

import scala.util.Random

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
class BindingsBenchmark:
   case class div_t(quot: Int, rem: Int) derives Struckt

   var rs: ResourceScope = _
   var segAlloc: SegmentAllocator = _
   var b: b_t = b_t(5, a_t(1,3))

   @Setup(Level.Iteration)
   def setup() =
      rs = ResourceScope.newConfinedScope
      segAlloc = SegmentAllocator.arenaAllocator(rs)


   @TearDown(Level.Iteration)
   def teardown() =
      rs.close

   val jnrLibC = LibraryLoader.create(classOf[JNRLibC]).load("c")
   val jnaLibC = JNALibC.INSTANCE

   object NativeCacheBased:
      def getpid(): Long = bind
      def div(numerator: Int, denominator: Int)(using SegmentAllocator): div_t =
         bind

      def strlen(string: String)(using SegmentAllocator): Int = bind

   @Benchmark
   def strlenSlincBench =
      given SegmentAllocator = segAlloc
      NativeCacheBased.strlen("hello world")

   @Benchmark
   def divSlincBench =
      given SegmentAllocator = segAlloc
      NativeCacheBased.div(5, 2)

   @Benchmark
   def getpidSlincBench =
      NativeCacheBased.getpid()

   @Benchmark
   def getpidJNRBench =
      jnrLibC.getpid()

   @Benchmark
   def strlenJNRBench =
      jnrLibC.strlen("hello world")

   @Benchmark
   def getpidJNABench =
      jnaLibC.getpid()

   @Benchmark
   def strlenJNABench =
      jnaLibC.strlen("hello world")

   @Benchmark
   def divJNABench =
      jnaLibC.div(5, 2)

   @Benchmark
   def libTestModifyBench =
      given SegmentAllocator = segAlloc
      val input = b.copy(d = b.d.copy(a = 5))
      val result = LibTest.slinc_test_modify(input)
      result.d.a

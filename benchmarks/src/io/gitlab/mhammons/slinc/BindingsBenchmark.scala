package io.gitlab.mhammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import cats.catsInstancesForId
import scala.annotation.tailrec
import org.openjdk.jmh.annotations.Mode
import jnr.ffi.LibraryLoader
import io.gitlab.mhammons.polymorphics.MethodHandleHandler
import jdk.incubator.foreign.{SegmentAllocator, ResourceScope}

import NativeCache.given NativeCache

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
   @Param(Array("1", "100", "10000"))
   var reps: Int = _
   import Fd.int
   type div_t = Struct {
      val quot: int
      val rem: int
   }

   val jnrLibC = LibraryLoader.create(classOf[JNRLibC]).load("c")
   val jnaLibC = JNALibC.INSTANCE

   object NativeCacheBased:
      given NativeCache = NativeCache()
      def getpid(): Long = bind
      def div(numerator: Int, denominator: Int)(using SegmentAllocator): div_t =
         bind

      def strlen(string: String)(using SegmentAllocator): Int = bind

   @Benchmark
   def strlenSlincBench =
      scope {
         var count = reps
         while count > 0 do
            NativeCacheBased.strlen("hello world")
            count -= 1
      }

   @Benchmark
   def divSlincBench =
      scope {
         var count = reps
         while count > 0 do
            NativeCacheBased.div(5, 2)
            count -= 1
      }

   @Benchmark
   def getpidSlincBench =
      var count = reps
      while count > 0 do
         NativeCacheBased.getpid()
         count -= 1

   @Benchmark
   def getpidJNRBench =
      var count = reps
      while count > 1 do
         jnrLibC.getpid()
         count -= 1
      jnrLibC.getpid()

   @Benchmark
   def strlenJNRBench =
      var count = reps
      while count > 1 do
         jnrLibC.strlen("hello world")
         count -= 1
      jnrLibC.strlen("hello world")

   @Benchmark
   def getpidJNABench =
      var count = reps
      while count > 0 do
         jnaLibC.getpid()
         count -= 1

   @Benchmark
   def strlenJNABench =
      var count = reps
      while count > 0 do
         jnaLibC.strlen("hello world")
         count -= 1

   @Benchmark
   def divJNABench =
      var count = reps
      while count > 0 do
         jnaLibC.div(5, 2)
         count -= 1

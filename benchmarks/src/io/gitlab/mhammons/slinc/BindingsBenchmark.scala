package io.gitlab.mhammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import cats.catsInstancesForId
import scala.annotation.tailrec
import org.openjdk.jmh.annotations.Mode
import jnr.ffi.LibraryLoader

@State(Scope.Thread)
class BindingsBenchmark:
   @Param(Array("1", "100", "10000"))
   var reps: Int = _
   val getpid = NativeIO.function[() => Long]("getpid")
   val strlen = NativeIO.function[String => Int]("strlen")
   import Fd.int
   type div_t = Struct {
      val quot: int
      val rem: int
   }
   val div = NativeIO.function[(Int, Int) => div_t]("div")

   val jnrLibC = LibraryLoader.create(classOf[JNRLibC]).load("c")
   val jnaLibC = JNALibC.INSTANCE

   @tailrec
   final def nativeIORepeat[A](io: NativeIO[A], remaining: Int)(
       res: NativeIO[A] = io
   ): NativeIO[A] =
      if remaining > 1 then
         nativeIORepeat(io, remaining - 1)(res.flatMap(_ => io))
      else res

   @Benchmark
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
   def strlenSlincBench =
      NativeIO
         .scope(nativeIORepeat(strlen("hello world"), reps)())
         .foldMap(NativeIO.impureCompiler)

   @Benchmark
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
   def divSlincBench =
      NativeIO
         .scope(nativeIORepeat(div(5, 2), reps)())
         .foldMap(NativeIO.impureCompiler)

   @Benchmark
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
   def getpidSlincBench =
      nativeIORepeat(getpid(), reps)().foldMap(NativeIO.impureCompiler)

   @Benchmark
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
   def getpidJNRBench =
      var count = reps
      while count > 1 do
         jnrLibC.getpid()
         count -= 1
      jnrLibC.getpid()

   @Benchmark
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
   def strlenJNRBench =
      var count = reps
      while count > 1 do
         jnrLibC.strlen("hello world")
         count -= 1
      jnrLibC.strlen("hello world")

   @Benchmark
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
   def getpidJNABench =
      var count = reps
      while count > 0 do
         jnaLibC.getpid()
         count -= 1

   @Benchmark
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
   def strlenJNABench =
      var count = reps
      while count > 0 do
         jnaLibC.strlen("hello world")
         count -= 1

   @Benchmark
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
   def divJNABench =
      var count = reps
      while count > 0 do
         jnaLibC.div(5, 2)
         count -= 1

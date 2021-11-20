package io.gitlab.mhammons.slinc

import scala.util.Random
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import java.util.Arrays
import scala.annotation.tailrec

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
   import Member.int
   type div_t = Struct {
      val a: int
      val b: int
   }
   type div_nested = Struct {
      val a: div_t
      val b: div_t
   }

   @Param(Array("1", "100", "10000"))
   var reps: Int = _

   @Benchmark
   def allocateSimple =
      scope {
         repeat(allocate[div_t](), reps)
      }

   @Benchmark
   def allocateNested =
      scope {
         repeat(allocate[div_nested](), reps)
      }

   @Benchmark
   def accessSimple =
      scope {
         val dt = allocate[div_t]()
         repeat(dt.a, reps)
      }

   @Benchmark
   def accessNested =
      scope {
         val dt = allocate[div_nested]()
         repeat(dt.a.a, reps)
      }

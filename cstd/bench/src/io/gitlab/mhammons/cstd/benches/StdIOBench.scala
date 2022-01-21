package io.gitlab.mhammons.cstd.benches

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import io.gitlab.mhammons.slinc.*
import io.gitlab.mhammons.cstd.*
import scala.util.chaining.*

@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED",
    "-Djmh.blackhole.autoDetect=true"
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class StdIOBench:
   scope { CLocale.setlocale(CLocale.LCAll, "C".serialize); () }

   val (i1, i2) = globalScope {
      allocate[Int](2).pipe(p => p -> (p + 1))
   }

   val f = globalScope {
      allocate[Int](1)
   }

   val data = globalScope {
      "1 2 3.0".serialize
   }

   val (formatConstant, buffer) = globalScope {
      "%d %d %f".serialize -> allocate[Byte](80)
   }

   @Benchmark
   def sprintf =
      scope {
         val format = "%d %d %f".serialize
         val buffer = allocate[Byte](80)
         StdIO.sprintf(buffer, format)(1, 2, 3.0f)
      }

   @Benchmark
   def sprintfNonAllocating =
      StdIO.sprintf(buffer, formatConstant)(1, 2, 3.0f)

   @Benchmark
   def scalaFormatString =
      f"${1}%d ${2}%d ${3.0f}%f"

   @Benchmark
   def sscanf =
      scope {
         val format = "%d %d %f".serialize
         val data = "1 2 3.0".serialize
         val ints = allocate[Int](2)
         val i = ints
         val i2 = ints + 1
         val float = allocate[Float](1)
         StdIO.sscanf(data, format)(i, i2, float)
      }

   @Benchmark
   def sscanfNonAllocating =
      StdIO.sscanf(data, formatConstant)(i1, i2, f)

   @Benchmark
   def scalaParseString =
      "1 2 3.0" match
         case s"${i1} ${i2} ${f}" =>
            i1.toInt
            i2.toInt
            f.toFloat

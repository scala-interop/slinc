package io.gitlab.mhammons.slinc

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import scala.util.Random
import io.gitlab.mhammons.slinc.components.MinimalPerfectHashtable

@State(Scope.Thread)
class Str:
   var strings: List[String] = _
   var values: List[Int] = _

   @Param(Array("0", "5", "15", "45", "100"))
   var numStrings: Int = _

   var map: Map[String, Int] = _

   var mpht: MinimalPerfectHashtable[Int] = _
   var pht: PerfectHashtable[Int] = _
   var key: String = _

   @Setup
   def setup =
      strings =
         if numStrings == 0 then
            List(
              "tm_sec",
              "tm_min",
              "tm_hour",
              "tm_mday",
              "tm_mon",
              "tm_year",
              "tm_wday",
              "tm_yday",
              "tm_isdst"
            )
         else
            List.fill(numStrings)(
              (0 until 5).map(_ => (Random.nextInt(26) + 65).toChar).mkString
            )
      values = (0 until strings.size).toList

      println("generating pht")
      pht = PerfectHashtable.runtimeConstruct(strings, values)
      println("done")
      println("generating mpht")
      mpht = MinimalPerfectHashtable.runtimeConstruct(strings, values)
      println("generating map")
      map = strings.zip(values).toMap
      println("done")
      key = Random.shuffle(strings).head

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
class BackingsBenchmark:
   @Benchmark
   def mapBench(str: Str) =
      repeatInl(str.map(str.key), 10000)
   @Benchmark
   def minimalPerfectHashTable(str: Str) =
      repeatInl(str.mpht(str.key), 10000)

   @Benchmark
   def perfectHashTable(str: Str) =
      repeatInl(str.pht(str.key), 10000)

   @Benchmark
   def ifThenElse(str: Str) =
      repeatInl(
        {
           if str.key == "tm_sec" then 0
           else if str.key == "tm_min" then 1
           else if str.key == "tm_hour" then 2
           else if str.key == "tm_mday" then 3
           else if str.key == "tm_mon" then 4
           else if str.key == "tm_year" then 5
           else if str.key == "tm_wday" then 6
           else if str.key == "tm_yday" then 7
           else 8
        },
        10000
      )

package io.gitlab.mhammons.slinc_benches

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import scala.util.Random
import io.gitlab.mhammons.slinc.Struct
import io.gitlab.mhammons.slinc.components.MinimalPerfectHashtable
import io.gitlab.mhammons.slinc.components.MinimalFastPerfectHashtable
import io.gitlab.mhammons.slinc.components.Member.int
import io.gitlab.mhammons.slinc.components.Primitives

@State(Scope.Thread)
class Str:
   var strings: List[String] = _
   var values: List[Int] = _

   @Param(Array("0", "5", "15", "45", "100"))
   var numStrings: Int = _

   var map: Map[String, Int] = _

   var mpht: MinimalPerfectHashtable[Int] = _
   var mfpht: MinimalFastPerfectHashtable[Int] = _
   var key: String = _
   var keys: Iterator[String] = _

   type div_t = Struct {
      val a: int
      val b: int
   }
   val layout = Primitives.Int

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
      println("done")
      println("generating mpht")
      mpht = MinimalPerfectHashtable.runtimeConstruct(strings, values)
      mfpht = MinimalFastPerfectHashtable.runtimeConstruct(strings, values)
      println("generating map")
      map = strings.zip(values).toMap
      key = Random.shuffle(strings).head
      val shuffledBig = (0 until 100_000).flatMap(_ => Random.shuffle(strings))

      keys = (0 until 200_000_000 / shuffledBig.size)
         .flatMap(_ => shuffledBig)
         .iterator
      println("done")

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
      val key = str.keys.next
      repeatInl(str.map(key), 10000)
   @Benchmark
   def minimalPerfectHashTable(str: Str) =
      val key = str.keys.next
      repeatInl(str.mpht(key), 10000)

   @Benchmark
   def minimalPowerPerfectHashtable(str: Str) =
      val key = str.keys.next
      repeatInl(str.mfpht(key), 10000)

   // @Benchmark
   // def summonVarhandleFromLayout(str: Str) = str.layout.varHandle(classOf[Int])

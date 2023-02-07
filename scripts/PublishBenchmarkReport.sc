using scala "3.2.2"

using lib "com.lihaoyi::upickle:2.0.0"

import upickle.default.ReadWriter
import upickle.default.read

import java.nio.file.{Paths, Files}

val os = args(1)
val jvm = args(0)
val benchmarkGroup = args(3)
val jit = args(4)

case class BenchmarkResults(benchmark: String, mode: String,  primaryMetric: PrimaryMetrics) derives ReadWriter

case class PrimaryMetrics(
  score: Double, 
  scoreError: Double,
  scoreUnit: String
) derives ReadWriter

val pathFragments = args(2).split('/')
val file = Paths.get(pathFragments.head, pathFragments.tail*)

val json = Files.readString(file)

val benchmarkResults = read[List[BenchmarkResults]](json)

println(s"$benchmarkGroup$jit")
val results = for 
  benchmark <- benchmarkResults
yield 
  val benchmarkName = benchmark.benchmark.foldLeft("")((str, c) =>
    if (str + c).contains(s"$benchmarkGroup$jit.") then "" else str + c
  )
  f"""||${benchmarkName}|${benchmark.mode}|${benchmark.primaryMetric.score}%.2f ± ${benchmark.primaryMetric.scoreError}%.2f ${benchmark.primaryMetric.scoreUnit}|""".stripMargin

val headTemplate = """||benchmark|mode|result|
          ||---|---|---|""".stripMargin
println(s"# $jvm - $os $benchmarkGroup$jit")
println((headTemplate :: results).mkString("\n"))
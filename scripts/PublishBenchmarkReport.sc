using scala "3.2.2"

using lib "com.lihaoyi::upickle:2.0.0"

import upickle.default.ReadWriter
import upickle.default.read

import java.nio.file.{Paths, Files}

val os = args(1)
val jvm = args(0)

case class BenchmarkResults(benchmark: String, primaryMetric: PrimaryMetrics) derives ReadWriter

case class PrimaryMetrics(
  score: Double, 
  scoreError: Double,
  scoreUnit: String
) derives ReadWriter

val pathFragments = args(2).split('/')
val file = Paths.get(pathFragments.head, pathFragments.tail*)

val json = Files.readString(file)

val benchmarkResults = read[List[BenchmarkResults]](json)

val results = for 
  benchmark <- benchmarkResults
yield 
  s"""||${benchmark.benchmark}|${benchmark.primaryMetric.score} ± ${benchmark.primaryMetric.scoreError} ${benchmark.primaryMetric.scoreUnit}|""".stripMargin

val headTemplate = """||benchmark|result|
          ||---|---|""".stripMargin
println(s"# $jvm - $os")
println((headTemplate :: results).mkString("\n"))
package fr.hammons.slinc

import org.openjdk.jmh.annotations.*,
Mode.{SampleTime, SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Throughput, SingleShotTime))
@Fork(
  jvmArgsAppend = Array(
    "--enable-preview",
    "--enable-native-access=ALL-UNNAMED"
    // "-XX:ActiveProcessorCount=1",
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class TransferBenchmarksStandard19 extends TransferBenchmarkShape(Slinc19.default)
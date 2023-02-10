package fr.hammons.slinc

import org.openjdk.jmh.annotations.*, Mode.{SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Throughput, SingleShotTime))
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED"
    // "-XX:ActiveProcessorCount=1",
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class TransferBenchmarksStandard extends TransferBenchmarkShape(Slinc17.default)

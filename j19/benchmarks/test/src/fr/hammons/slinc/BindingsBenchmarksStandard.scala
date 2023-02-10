package fr.hammons.slinc

import org.openjdk.jmh.annotations.*, Mode.{SingleShotTime, Throughput}
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
class BindingsBenchmarksStandard extends BindingsBenchmarkShape(Slinc19.default)

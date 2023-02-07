package fr.hammons.slinc

import org.openjdk.jmh.annotations.*,
  Mode.{SampleTime, SingleShotTime, Throughput}
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
class BindingsBenchmarksNoJIT extends BindingsBenchmarkShape(Slinc17.noJit)

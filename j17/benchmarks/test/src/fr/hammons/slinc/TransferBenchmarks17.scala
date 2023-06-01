package fr.hammons.slinc

import org.openjdk.jmh.annotations.*, Mode.{SingleShotTime, AverageTime}
import java.util.concurrent.TimeUnit
import jdk.incubator.foreign.*
import jdk.incubator.foreign.CLinker.*
import org.openjdk.jmh.infra.Blackhole
import fr.hammons.slinc.types.*

@State(Scope.Thread)
@BenchmarkMode(Array(AverageTime, SingleShotTime))
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED"
    // "-XX:ActiveProcessorCount=1",
  )
)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class TransferBenchmarks17 extends TransferBenchmarkShape(Slinc17.noJit) {
  case class H(a: Int, b: Float, c: CLong)

  val rs = ResourceScope.globalScope().nn 
  val segAlloc = SegmentAllocator.arenaAllocator(rs).nn
  val ml = MemoryLayout.structLayout(C_INT, C_FLOAT, C_LONG)

  val ms = segAlloc.allocate(ml)
  val h = H(1,2f,CLong(3))

  val writerFn = (ms: MemorySegment | Null, offset: Bytes, value: H) => 
      MemoryAccess.setIntAtOffset(ms, offset.toLong + 0, h.a)
      MemoryAccess.setFloatAtOffset(ms, offset.toLong + 4, h.b)
      MemoryAccess.setLongAtOffset(ms, offset.toLong + 8, a.c.asInstanceOf[Long])



  @Benchmark
  def writeManual(blackhole: Blackhole) = blackhole.consume(
    writerFn(ms, Bytes(0), h)
  )

}

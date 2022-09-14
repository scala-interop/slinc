package fr.hammons.sffi

import org.openjdk.jmh.annotations.*, Mode.{SampleTime, SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.*
import jdk.incubator.foreign.CLinker.*
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.codeOf


//multicore results:
// AssignBenches.assignCaseClass      thrpt    5      52.865 ± 2.990  ops/us
// AssignBenches.assignCaseClass2     thrpt    5     185.211 ± 6.678  ops/us
// AssignBenches.assignCaseClass         ss         2260.944           us/op
// AssignBenches.assignCaseClass2        ss        11733.409           us/op
// AssignBenches.assignCaseClass2SST     ss       814368.424           us/op
@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED",
    //"-Denable-sffi-jit=true",
    "-XX:ActiveProcessorCount=1",
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@BenchmarkMode(Array(Mode.SampleTime))
class AssignBenches:
  val ffi = FFI.getFFI
  import ffi.*
  val intPtr = Scope(global = true) {
    Ptr.blank[Int](1)
  }

  val ffi3 = FFI173
  import ffi3.*
  case class X(a: Int, y: Y, b: Int)
  case class Y(a: Int, b: Int)

  val layout: Object = layoutOf[X].asInstanceOf[MemoryLayout]
  val xVal = X(2,Y(2,3),3)

  //println(layoutOf[X])
  val size = layoutOf[X].asInstanceOf[MemoryLayout].byteSize()
  val mem =
    CLinker
      .allocateMemory(size)
      .nn
      .asSegment(size, ResourceScope.globalScope())
      .nn.asInstanceOf[basics.RawMem]

  // val gen = 
  //   val layout = MemoryLayout.structLayout(C_INT, MemoryLayout.structLayout(C_INT, C_INT),C_INT).nn
  //   () => 
  //   Write17.genStructWriter(layout)
  //   ()

  // FFI.getFFI

  case class div_t(quot: Int, u: div_u, rem: Int) derives Struct
  case class div_u(a: Int, b: Int) derives Struct

  val div_tPtr = Scope(global = true) {
    Ptr.blank[div_t](1)
  }

  val div_t_Value = div_t(2, div_u(2,3), 3)

  @Benchmark
  def assignInt = intPtr.update(4)

  @Benchmark
  @BenchmarkMode(Array(SingleShotTime, SampleTime))
  def assignCaseClass = div_tPtr.update(div_t_Value)

  @Benchmark
  def derefCaseClass = !div_tPtr

  @Benchmark
  @BenchmarkMode(Array(SingleShotTime, Throughput))
  def assignCaseClass2 = 
    write(mem, 0, xVal)

  @Benchmark 
  @BenchmarkMode(Array(SingleShotTime, SampleTime))
  def compile(blackhole: Blackhole) = 
    blackhole.consume(Write17.gen(layout))

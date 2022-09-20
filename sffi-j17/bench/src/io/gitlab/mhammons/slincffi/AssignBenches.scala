package fr.hammons.sffi

import org.openjdk.jmh.annotations.*,
Mode.{SampleTime, SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.*
import jdk.incubator.foreign.CLinker.*
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.codeOf
import scala.deriving.Mirror


val ffi3 = FFI173
import ffi3.{Struct as Struct2, *, given}
case class Y(a: Int, y: Int) derives Struct2
case class X(a: Int, y: Y, b: Int) derives Struct2



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
    "--enable-native-access=ALL-UNNAMED"
    // "-XX:ActiveProcessorCount=1",
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




  val xVal = X(2, Y(2, 3), 3)

  // println(layoutOf[X])
  val size = summon[LayoutOf[X]].layout.size
  val mem = Allocator.globalAllocator().allocate(summon[LayoutOf[X]].layout)

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

  val div_t_Value = div_t(2, div_u(2, 3), 3)

  @Benchmark
  def assignInt = intPtr.update(4)

  @Benchmark
  @BenchmarkMode(Array(SingleShotTime, Throughput))
  def assignCaseClass = div_tPtr.update(div_t_Value)

  @Benchmark
  def derefCaseClass = !div_tPtr


  @Benchmark 
  @BenchmarkMode(Array(SingleShotTime, Throughput))
  def assignInt2 = 
    mem.write(4, 0.toBytes)

  @Benchmark
  @BenchmarkMode(Array(SingleShotTime, Throughput))
  def assignCaseClass2 =
    mem.write(xVal, 0.toBytes)

  @Benchmark
  @BenchmarkMode(Array(SingleShotTime, Throughput))
  def readCaseClass2 = 
    mem.read[X](0.toBytes)

  @Benchmark 
  @BenchmarkMode(Array(SingleShotTime, Throughput))
    @Fork(
    jvmArgsAppend = Array(
      "--add-modules=jdk.incubator.foreign",
      "--enable-native-access=ALL-UNNAMED",
      "-Dsffi-jit=false"
      // "-XX:ActiveProcessorCount=1",
    )
  )
  def readCaseClass2NoJIT =
    mem.read[X](0.toBytes)

  @Benchmark 
  def genCaseClass =
    val t = (2,(2,3),3)
    val m1 = summon[Mirror.ProductOf[X]]
    val m2 = summon[Mirror.ProductOf[Y]]
    m1.fromProduct(t.copy(_2 = m2.fromProduct(t._2)))

  @Benchmark
  @BenchmarkMode(Array(SingleShotTime, Throughput))
  @Fork(
    jvmArgsAppend = Array(
      "--add-modules=jdk.incubator.foreign",
      "--enable-native-access=ALL-UNNAMED",
      "-Dsffi-jit=false"
      // "-XX:ActiveProcessorCount=1",
    )
  )
  def assignCaseClass2NoJIT =
    mem.write(xVal, 0.toBytes)
// @Benchmark
// @BenchmarkMode(Array(SingleShotTime, SampleTime))
// def compile(blackhole: Blackhole) =
//   blackhole.consume(Write17.gen(layout))

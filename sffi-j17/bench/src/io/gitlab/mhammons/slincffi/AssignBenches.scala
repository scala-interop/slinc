package fr.hammons.sffi

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED",
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class AssignBenches:
  val ffi: FFI = FFI17
  import ffi.*
  val intPtr = Scope(global = true) {
    Ptr.blank[Int](1)
  }


  case class div_t(quot: Int, rem: Int) derives Struct

  val div_tPtr = Scope(global = true) {
    Ptr.blank[div_t](1)
  }

  val div_t_Value = div_t(4,2)

  @Benchmark
  def assignInt = intPtr.update(4)

  @Benchmark
  def assignCaseClass = div_tPtr.update(div_t_Value)

  @Benchmark
  def derefCaseClass = !div_tPtr
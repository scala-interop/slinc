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
class LayoutBenches:
  val ffi1 = FFI.getFFI
  val ffi2 = FFI173
  import ffi2.{Struct as Struct2, given}

  import ffi1.*
  case class X(a: Int, b: Int) derives Struct
  case class Y(a: Int, b: Int) derives Struct2
  @Benchmark
  def ffi1Layout = 
    summon[LayoutInfo[X]]

  @Benchmark 
  def ffi2Layout =
    import ffi2.*
    summon[LayoutOf[Y]]
package fr.hammons.sffi

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import scala.util.Random

@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED"
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class BindingBenches:
  val ffi: FFI = FFI17
  import ffi.*

  case class div_t(quot: Int, rem: Int) derives Struct

  val abs = fnGen[Int => Int]("abs")
  val labs = fnGen[Int => Int]("labs")
  val rand = fnGen[() => Int]("rand")
  val sprintf = fnGen[(Ptr[Byte], Ptr[Byte], Seq[Variadic]) => Int]("sprintf")
  val div = fnGen[(Int, Int) => div_t]("div")
  val qsort =
    fnGen[(Ptr[Any], Long, Long, Ptr[(Ptr[Any], Ptr[Any]) => Int]) => Unit](
      "qsort"
    )

  def qsortMeth[A](
      arr: Ptr[A],
      elemSize: Long,
      elemNumber: Long,
      sort: Ptr[(Ptr[A], Ptr[A]) => Int]
  ): Unit = qsort(
    arr.as[Any],
    elemSize,
    elemNumber,
    sort.as[(Ptr[Any], Ptr[Any]) => Int]
  )

  @Benchmark
  def nativeAbs = abs(-5)

  @Benchmark
  def scalaAbs = Math.abs(-5)

  @Benchmark
  def nativeRand = rand()

  @Benchmark
  def scalaRand = Random.nextInt()

  val res = "hello world".getBytes().nn

  @Benchmark
  def nativeSprintf = Scope() {
    val res = Ptr.blank[Byte](256)
    val str = toNative("hello %s")
    val ptr = sprintf(res, str, Seq(toNative("world")))
  }

  @Benchmark
  def nativeDiv =
    div(4, 2)

  val arrayToSort = Array.fill(10000)(Random.nextInt)
  val fnPtr: Ptr[(Ptr[Int], Ptr[Int]) => Int] = Scope(global = true) {
    toNative { (a, b) =>
      val aVal = !a
      val bVal = !b
      if aVal < bVal then -1
      else if aVal == bVal then 0
      else 1
    }
  }
  @Benchmark
  def nativeQsort =
    Scope() {
      val arr = toNative(arrayToSort)

      qsortMeth(arr, 4, 10000, fnPtr)
    }

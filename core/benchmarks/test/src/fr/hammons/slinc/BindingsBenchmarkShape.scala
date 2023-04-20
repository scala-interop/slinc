package fr.hammons.slinc

import org.openjdk.jmh.annotations.{Scope as JmhScope, *}
import java.util.concurrent.TimeUnit
import scala.util.Random
import fr.hammons.slinc.types.*
import scala.annotation.nowarn
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.uninitialized
@State(JmhScope.Thread)
class QSortState {
  var values: Array[Int] = uninitialized
  @Setup
  def setup(): Unit =
    values = Array.fill(10_000)(scala.util.Random.nextInt())

}

case class div_t(quot: CInt, rem: CInt)

@nowarn("msg=unused import")
trait BindingsBenchmarkShape(val s: Slinc):
  import scala.language.unsafeNulls
  trait Cstd derives FSet:
    def abs(i: CInt): CInt
    def div(numer: CInt, denom: CInt): div_t
    def labs(l: CLong): CLong
    def qsort(
        array: Ptr[Nothing],
        num: SizeT,
        size: SizeT,
        fn: Ptr[(Ptr[Nothing], Ptr[Nothing]) => Int]
    ): Unit

  trait Cstd2 derives FSet:
    def labs(l: Long): Long

  import s.given

  val cstd = FSet.instance[Cstd]
  val cstd2 = FSet.instance[Cstd2]

  given Struct[div_t] = Struct.derived

  import s.{given, *}

  val upcall: Ptr[(Ptr[Nothing], Ptr[Nothing]) => Int] = Scope.global {
    Ptr.upcall((a, b) =>
      val aVal = !a.castTo[Int]
      val bVal = !b.castTo[Int]
      if aVal < bVal then -1
      else if aVal == bVal then 0
      else 1
    )
  }

  val path = Random.nextBoolean()

  @Benchmark
  def abs =
    cstd.abs(6)

  val clong = CLong(-15)
  @Benchmark
  def labs =
    cstd.labs(clong)

  @Benchmark
  def labs2 =
    cstd2.labs(-15)

  @Benchmark
  def div =
    cstd.div(5, 2)

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def qsort(state: QSortState, bh: Blackhole): Unit =
    bh.consume {
      for size <- SizeT.maybe(10000)
      do
        Scope.confined {
          val sortingArr = Ptr.copy(state.values).castTo[Nothing]
          cstd.qsort(
            sortingArr,
            size,
            IntDescriptor.size.toSizeT,
            upcall
          )
          sortingArr.castTo[Int].asArray(10000)
        }
    }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def scalasort(state: QSortState): Array[Int] =
    state.values.sorted

package fr.hammons.slinc

import org.openjdk.jmh.annotations.{Scope as _, *}
import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.annotation.nowarn
import fr.hammons.slinc.modules.LibModule

case class div_t(quot: Int, rem: Int)

trait BindingsBenchmarkShape(val s: Slinc):
  import scala.language.unsafeNulls

  trait Cstd3 derives Lib:
    def abs(i: Int): Int

  import s.given LibModule

  @Benchmark
  def abs2 =
    Lib.instance[Cstd3].abs(-4)

  lazy val cstd3 = Lib.instance[Cstd3]

  @Benchmark
  def abs3 =
    cstd3.abs(-4)

  import s.{given, *}

  object Cstd derives Library:
    def abs(i: Int): Int = Library.binding
    def div(numer: Int, denom: Int): div_t = Library.binding
    def labs(l: CLong): CLong = Library.binding
    // todo: needs SizeT
    def qsort[A](
        array: Ptr[A],
        num: Long,
        size: SizeT,
        fn: Ptr[(Ptr[A], Ptr[A]) => A]
    ): Unit = Library.binding

  object Cstd2 derives Library:
    def labs(l: Long): Long = Library.binding

  given Struct[div_t] = Struct.derived

  val lib = summon[Library[Cstd.type]]
  val absHandle = lib.handles(0)

  val base = Seq.fill(10000)(Random.nextInt)
  val baseArr = base.toArray

  val upcall: Ptr[(Ptr[Int], Ptr[Int]) => Int] = Scope.global {
    Ptr.upcall((a, b) =>
      val aVal = !a
      val bVal = !b
      if aVal < bVal then -1
      else if aVal == bVal then 0
      else 1
    )
  }

  @Benchmark
  def abs =
    Cstd.abs(6)

  @Benchmark
  def labs =
    Cstd.labs(-15.as[CLong])

  @Benchmark
  def labs2 =
    Cstd2.labs(-15)

  @Benchmark
  def div =
    Cstd.div(5, 2)

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def qsort =
    Scope.confined {
      val sortingArr = Ptr.copy(baseArr)
      Cstd.qsort(
        sortingArr,
        10000,
        4.as[SizeT],
        Ptr.upcall((a, b) =>
          val aVal = !a
          val bVal = !b
          if aVal < bVal then -1
          else if aVal == bVal then 0
          else 1
        )
      )
      sortingArr.asArray(10000)
    }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def scalasort =
    baseArr.sorted

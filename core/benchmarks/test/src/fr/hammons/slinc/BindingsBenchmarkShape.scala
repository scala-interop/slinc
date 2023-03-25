package fr.hammons.slinc

import org.openjdk.jmh.annotations.{Scope as _, *}
import java.util.concurrent.TimeUnit
import scala.util.Random
import fr.hammons.slinc.types.*
import scala.annotation.nowarn

case class div_t(quot: CInt, rem: CInt)

@nowarn("msg=unused import")
trait BindingsBenchmarkShape(val s: Slinc):
  import scala.language.unsafeNulls
  trait Cstd derives Lib:
    def abs(i: CInt): CInt
    def div(numer: CInt, denom: CInt): div_t
    def labs(l: CLong): CLong
    def qsort(
        array: Ptr[Nothing],
        num: SizeT,
        size: SizeT,
        fn: Ptr[(Ptr[Nothing], Ptr[Nothing]) => Int]
    ): Unit

  trait Cstd2 derives Lib:
    def labs(l: Long): Long

  import s.given

  val cstd = Lib.instance[Cstd]
  val cstd2 = Lib.instance[Cstd2]

  given Struct[div_t] = Struct.derived

  val base = Seq.fill(10000)(Random.nextInt)
  val baseArr = base.toArray

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

  @Benchmark
  def labs =
    cstd.labs(CLong(-15))

  @Benchmark
  def labs2 =
    cstd2.labs(-15)

  @Benchmark
  def div =
    cstd.div(5, 2)

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def qsort =
    for size <- SizeT.maybe(10000)
    do
      Scope.confined {
        val sortingArr = Ptr.copy(baseArr).castTo[Nothing]
        cstd.qsort(
          sortingArr,
          size,
          IntDescriptor.size.toSizeT,
          upcall
        )
        sortingArr.castTo[Int].asArray(10000)
      }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def scalasort =
    baseArr.sorted

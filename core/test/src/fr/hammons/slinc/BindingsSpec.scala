package fr.hammons.slinc

import fr.hammons.slinc.BindingsSpec.div_t
import scala.util.Random
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import fr.hammons.slinc.types.OS

trait BindingsSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given, *}

  object Cstd derives Library:
    def abs(a: Int): Int = Library.binding
    def labs(l: CLong): CLong = Library.binding
    def div(a: Int, b: Int): div_t = Library.binding
    def rand(): Int = Library.binding
    def qsort[A](
        array: Ptr[A],
        num: SizeT,
        size: SizeT,
        fn: Ptr[(Ptr[A], Ptr[A]) => Int]
    ): Unit = Library.binding
    def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Variadic*): Unit =
      Library.binding

  given Struct[div_t] = Struct.derived

  property("abs gives back absolute integers") {
    forAll { (i: Int) =>
      assertEquals(Cstd.abs(i), i.abs)
    }
  }

  test("abs") {
    assertEquals(Cstd.abs(-4), 4)
  }

  property("labs gives back absolute CLongs") {
    forAll(Gen.choose(Int.MinValue + 1, Int.MaxValue)) { (i: Int) =>
      assertEquals(Cstd.labs(i.as[CLong]).as[Long], i.toLong.abs)
    }
  }

  test("labs") {
    platformFocus(types.x64.Linux){
      assertEquals(Cstd.labs(-13l): Long, 13l)
    }.getOrElse(assert(false, os))
  }

  property("div calculates quotient and remainder") {
    val validIntRange =
      Gen.oneOf(Gen.choose(Int.MinValue + 1, -1), Gen.choose(1, Int.MaxValue))
    forAll(validIntRange, validIntRange) { (a: Int, b: Int) =>
      val result = Cstd.div(a, b)

      assertEquals(result.quot, a / b)
      assertEquals(result.rem, a % b)
    }
  }

  test("div") {
    assertEquals(Cstd.div(5, 2), div_t(2, 1))
  }

  test("rand") {
    assertNotEquals(Cstd.rand(), Cstd.rand())
  }

  property("qsort should sort") {
    forAll { (arr: Array[Int]) =>
      Scope.confined {
        val cArr = Ptr.copy(arr)

        Cstd.qsort(
          cArr,
          arr.size.as[SizeT],
          4.as[SizeT],
          Ptr.upcall((a, b) =>
            val aVal = !a
            val bVal = !b
            if aVal < bVal then -1
            else if aVal == bVal then 0
            else 1
          )
        )

        assertEquals(cArr.asArray(arr.length).toSeq, arr.sorted.toSeq)
      }
    }

  }
  test("qsort") {
    val testArray = Random.shuffle(Array.fill(1024)(Random.nextInt())).toArray
    Scope.confined {
      val arr = Ptr.copy(testArray)

      Cstd.qsort(
        arr,
        testArray.size.as[SizeT],
        4.as[SizeT],
        Ptr.upcall((a, b) =>
          val aVal = !a
          val bVal = !b
          if aVal < bVal then -1
          else if aVal == bVal then 0
          else 1
        )
      )

      assertEquals(arr.asArray(testArray.size).toSeq, testArray.sorted.toSeq)
    }
  }

  property("sprintf should format") {
    forAll(Arbitrary.arbitrary[Int], Gen.asciiPrintableStr) { (i, s) =>
      Scope.confined {
        val format = Ptr.copy("%i %s")
        val buffer = Ptr.blankArray[Byte](256)

        // ascii chars only
        assertEquals(format.copyIntoString(200), "%i %s")
        Cstd.sprintf(buffer, format, i, Ptr.copy(s))
        assertEquals(buffer.copyIntoString(256), s"$i $s")
      }
    }
  }

  test("sprintf") {
    Scope.confined {
      val format = Ptr.copy("%i hello: %s %i")
      val buffer = Ptr.blankArray[Byte](256)

      assertEquals(format.copyIntoString(200), "%i hello: %s %i")
      Cstd.sprintf(buffer, format, 1, Ptr.copy("hello"), 2)
      assertEquals(buffer.copyIntoString(256), "1 hello: hello 2")
    }
  }

  test("time") {

    val current = System.currentTimeMillis() / 1000
    val time = if os == OS.Windows then
      object Time derives Library:
        def _time64(timer: Ptr[TimeT]): TimeT = Library.binding

      Time._time64(Null[TimeT])
    else
      object Time derives Library:
        def time(timer: Ptr[TimeT]): TimeT = Library.binding

      Time.time(Null[TimeT])

    assert(
      time.maybeAs[Long].map(_ - current).map(_.abs).forall(_ < 5),
      time.maybeAs[Long].map(_ - current).map(_.abs)
    )
  }

object BindingsSpec:
  case class div_t(quot: Int, rem: Int)

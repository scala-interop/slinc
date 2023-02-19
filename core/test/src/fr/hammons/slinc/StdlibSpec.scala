package fr.hammons.slinc

import fr.hammons.slinc.StdlibSpec.div_t
import scala.util.Random
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import fr.hammons.slinc.types.OS
import scala.annotation.nowarn

//todo: remove when https://github.com/lampepfl/dotty/issues/16876 is fixed
trait StdlibSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given, *}

  object Cstd derives Library:
    def abs(a: Int): Int = Library.binding
    def labs(l: CLong): CLong = Library.binding
    def div(a: Int, b: Int): div_t = Library.binding
    // def ldiv(a: Long, b: Long): ldiv_t = Library.binding
    // def lldiv(a: Long, b: Long): lldiv_t = Library.binding
    def rand(): Int = Library.binding
    def qsort[A](
        array: Ptr[A],
        num: SizeT,
        size: SizeT,
        fn: Ptr[(Ptr[A], Ptr[A]) => Int]
    ): Unit = Library.binding
    def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Variadic*): Unit =
      Library.binding
    def atof(str: Ptr[Byte]): CDouble = Library.binding
    def strtod(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]): CDouble =
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
    platformFocus(x64.Linux) {
      forAll(Gen.choose(Long.MinValue + 1, Long.MaxValue)) { (l: Long) =>
        assertEquals(Cstd.labs(l): Long, l.abs)

      }
    }.orElse(
      platformFocus(x64.Mac) {
        forAll(Gen.choose(Long.MinValue + 1, Long.MaxValue)) { (l: Long) =>
          assertEquals(Cstd.labs(l): Long, l.abs)
        }
      }
    ).orElse(
      platformFocus(x64.Windows) {
        forAll(Gen.choose(Int.MinValue + 1, Int.MaxValue)) { (i: Int) =>
          assertEquals(Cstd.labs(i): Int, i.abs)
        }
      }
    ).getOrElse(assume(false, os))
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

  /*
  test("ldiv") {
    assertEquals(Cstd.ldiv(5L, 2L), ldiv_t(2L, 1L))
  }

  test("lldiv") {
    assertEquals(Cstd.lldiv(5L, 2L), lldiv_t(2L, 1L))
  }
   */
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

  property("atof convert strings to floats") {
    forAll { (d: Double) =>
      Scope.confined {
        val pStr = Ptr.copy(f"$d%f")
        assertEqualsDouble(Cstd.atof(pStr), d, 0.1)
      }
    }
  }

  property("strtod convert doubles from string") {
    forAll { (d: Double) =>
      Scope.confined {
        val input = f"$d%f $d%f"
        val maxSize = input.length()
        val pStr0 = Ptr.copy(input)

        val ans1 = Ptr.blank[Ptr[Byte]]
        val a1 = Cstd.strtod(pStr0, ans1)
        assertEqualsDouble(a1, d, 0.1)
        val pStr1 = !ans1
        val r1 = pStr1.copyIntoString(maxSize)
        assertEquals(r1, f" $d%f")

        val ans2 = Ptr.blank[Ptr[Byte]]
        val a2 = Cstd.strtod(pStr1, ans2)
        assertEqualsDouble(a2, d, 0.1)
        assertEquals(!(!ans2), 0.toByte)
      }
    }
  }

  test("strtod convert bad string") {
    Scope.confined {
      val input = "notPossible"
      val maxSize = input.length()
      val pStr0 = Ptr.copy(input)

      val ans1 = Ptr.blank[Ptr[Byte]]
      val a = Cstd.strtod(pStr0, ans1)
      assertEqualsDouble(a, 0.0d, 0.1)
      val pStr1 = !ans1
      val r1 = pStr1.copyIntoString(maxSize)
      assertEquals(r1, input)
    }
  }

object StdlibSpec:
  case class div_t(quot: Int, rem: Int)
  case class ldiv_t(quot: Long, rem: Long)
  case class lldiv_t(quot: Long, rem: Long)

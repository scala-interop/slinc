package fr.hammons.slinc

import scala.util.Random
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import fr.hammons.slinc.types.CInt
import fr.hammons.slinc.types.{CLong, SizeT, TimeT}
import fr.hammons.slinc.types.CChar
import fr.hammons.slinc.types.CDouble
import fr.hammons.slinc.types.IntegralAlias
import types.{OS, Arch}
import fr.hammons.slinc.annotations.NameOverride
import fr.hammons.slinc.types.CLongLong

trait StdlibSpec(val slinc: Slinc) extends ScalaCheckSuite:
  case class div_t(quot: CInt, rem: CInt) derives Struct
  case class ldiv_t(quot: CLong, rem: CLong) derives Struct
  case class lldiv_t(quot: CLongLong, rem: CLongLong) derives Struct

  trait Cstd derives FSet:
    def abs(a: CInt): CInt
    def labs(l: CLong): CLong
    def div(a: CInt, b: CInt): div_t
    def ldiv(a: CLong, b: CLong): ldiv_t
    def lldiv(a: CLongLong, b: CLongLong): lldiv_t
    def rand(): CInt
    def qsort(
        array: Ptr[Nothing],
        num: SizeT,
        size: SizeT,
        fn: Ptr[(Ptr[Nothing], Ptr[Nothing]) => CInt]
    ): Unit
    def sprintf(ret: Ptr[CChar], string: Ptr[CChar], args: Seq[Variadic]): Unit
    def atof(str: Ptr[CChar]): CDouble
    def strtod(str: Ptr[CChar], endptr: Ptr[Ptr[CChar]]): CDouble
    @NameOverride("_time64", (OS.Windows, Arch.X64))
    def time(timer: Ptr[TimeT]): TimeT

  import slinc.{Null, given}

  val cstd = FSet.instance[Cstd]

  property("abs gives back absolute integers"):
      forAll: (i: Int) =>
        assertEquals(cstd.abs(i), i.abs)

  test("abs"):
      assertEquals(cstd.abs(-4), 4)

  val clongChoose =
    Gen
      .choose(IntegralAlias.min[CLong], IntegralAlias.max[CLong])
      .map(CLong.maybe)
      .map(_.get)

  property("labs gives back absolute CLongs"):
      forAll(clongChoose): (c: CLong) =>
        assertEquals(
          IntegralAlias.toLong(cstd.labs(c)),
          IntegralAlias.toLong(c).abs
        )

  property("div calculates quotient and remainder"):
      val validIntRange =
        Gen.oneOf(Gen.choose(Int.MinValue + 1, -1), Gen.choose(1, Int.MaxValue))
      forAll(validIntRange, validIntRange): (a: Int, b: Int) =>
        val result = cstd.div(a, b)

        assertEquals(result.quot, a / b)
        assertEquals(result.rem, a % b)

  test("div"):
      assertEquals(cstd.div(5, 2), div_t(2, 1))

  // property("ldiv calculates quotient and remainder"):
  //   forAll(clongChoose, clongChoose): (a: CLong, b: CLong) =>
  //     val result = cstd.ldiv(a,b)

  //     assertEquals(IntegralAlias.toLong(result.quot), IntegralAlias.toLong(a) / IntegralAlias.toLong(b))
  //     assertEquals(IntegralAlias.toLong(result.rem), IntegralAlias.toLong(a) % IntegralAlias.toLong(b))

  test("ldiv"):
      assertEquals(cstd.ldiv(CLong(5), CLong(2)), ldiv_t(CLong(2), CLong(1)))

  // property("lldiv calculates quotient and remainder"):
  //   forAll: (a: CLongLong, b: CLongLong) =>
  //     val result = cstd.lldiv(a,b)

  //     assertEquals(result.quot, a / b)
  //     assertEquals(result.rem, a % b)

  test("lldiv") {
    assertEquals(cstd.lldiv(5L, 2L), lldiv_t(2L, 1L))
  }
  test("rand"):
      assertNotEquals(cstd.rand(), cstd.rand())

  // todo: this should generate arrays of max length of SizeT
  property("qsort should sort"):
      forAll: (arr: Array[Int]) =>
        Scope
          .confined:
            val cArr = Ptr.copy(arr).castTo[Nothing]

            SizeT
              .maybe(arr.length)
              .map: len =>
                cstd.qsort(
                  cArr,
                  len,
                  DescriptorOf[CInt].toForeignTypeDescriptor.size.toSizeT,
                  Ptr.upcall((a: Ptr[Nothing], b: Ptr[Nothing]) =>
                    val aVal = !a.castTo[CInt]
                    val bVal = !b.castTo[CInt]
                    if aVal < bVal then -1
                    else if aVal == bVal then 0
                    else 1
                  )
                )
                assertEquals(
                  cArr.castTo[Int].asArray(arr.length).toSeq,
                  arr.sorted.toSeq
                )
          .getOrElse(fail("Array too long for platform"))

  test("qsort"):
      val testArray = Random.shuffle(Array.fill(1024)(Random.nextInt())).toArray

      Scope.confined:
        val arr = Ptr.copy(testArray).castTo[Nothing]

        val size = SizeT.apply(1024: Short)

        cstd.qsort(
          arr,
          size,
          DescriptorOf[CInt].toForeignTypeDescriptor.size.toSizeT,
          Ptr.upcall: (a, b) =>
            val aVal = !a.castTo[CInt]
            val bVal = !b.castTo[CInt]
            if aVal < bVal then -1
            else if aVal == bVal then 0
            else 1
        )

        assertEquals(
          arr.castTo[CInt].asArray(testArray.size).toSeq,
          testArray.sorted.toSeq
        )
  // end qsort test

  property("sprintf should format"):
      forAll(Arbitrary.arbitrary[Int], Gen.asciiPrintableStr): (i, s) =>
        Scope.confined:
          val format = Ptr.copy("%i %s")
          val buffer = Ptr.blankArray[Byte](256)

          // ascii chars only
          assertEquals(format.copyIntoString(200), "%i %s")
          cstd.sprintf(buffer, format, Seq(i, Ptr.copy(s)))
          assertEquals(buffer.copyIntoString(256), s"$i $s")

  test("sprintf"):
      Scope.confined:
        val format = Ptr.copy("%i hello: %s %i")
        val buffer = Ptr.blankArray[Byte](256)

        assertEquals(format.copyIntoString(200), "%i hello: %s %i")
        cstd.sprintf(buffer, format, Seq(1, Ptr.copy("hello"), 2))
        assertEquals(buffer.copyIntoString(256), "1 hello: hello 2")

  test("time"):
      val current = System.currentTimeMillis() / 1000
      val result = cstd.time(Null[TimeT])
      assert(
        (IntegralAlias.toLong(result) - current).abs < 5
      )

  // end test time

  property("atof convert strings to floats"):
      forAll: (d: Double) =>
        Scope.confined:
          val pStr = Ptr.copy(f"$d%f")
          assertEqualsDouble(cstd.atof(pStr), d, 0.1)

  property("strtod convert doubles from string"):
      forAll: (d: Double) =>
        Scope.confined:
          val input = f"$d%f $d%f"
          val maxSize = input.length()
          val pStr0 = Ptr.copy(input)

          val ans1 = Ptr.blank[Ptr[Byte]]
          val a1 = cstd.strtod(pStr0, ans1)
          assertEqualsDouble(a1, d, 0.1)
          val pStr1 = !ans1
          val r1 = pStr1.copyIntoString(maxSize)
          assertEquals(r1, f" $d%f")

          val ans2 = Ptr.blank[Ptr[Byte]]
          val a2 = cstd.strtod(pStr1, ans2)
          assertEqualsDouble(a2, d, 0.1)
          assertEquals(!(!ans2), 0.toByte)

  test("strtod convert bad string"):
      Scope.confined:
        val input = "notPossible"
        val maxSize = input.length()
        val pStr0 = Ptr.copy(input)

        val ans1 = Ptr.blank[Ptr[Byte]]
        val a = cstd.strtod(pStr0, ans1)
        assertEqualsDouble(a, 0.0d, 0.1)
        val pStr1 = !ans1
        val r1 = pStr1.copyIntoString(maxSize)
        assertEquals(r1, input)

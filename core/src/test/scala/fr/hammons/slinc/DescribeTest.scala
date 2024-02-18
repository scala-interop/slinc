package fr.hammons.slinc

import fr.hammons.slinc.internal.Describe
import compiletime.{codeOf, error}

class DescribeTest extends munit.FunSuite:
  test("Describe[Boolean] == Describe[Byte]") {
    assert(summon[Describe[Boolean]].apply == summon[Describe[Byte]].apply)
  }

  test("Describe[Boolean] != Describe[Int]") {
    assert(summon[Describe[Boolean]].apply != summon[Describe[Int]].apply)
  }

  test("lala") {
    given Runtime = new Runtime:
      def platform: Platform = Platform.LinuxX64
    // summon[Numeric[CLong]]

    // summon[Numeric[CLong]]

    val x = fr.hammons.slinc.experimental.CLong(-5)
    println(CInt(5))

    x.toInt
    assertEquals(x.abs.toInt, 5)

    // lo.map(_.toString())

    // given Platform.LinuxX64.type = Platform.LinuxX64

    // ong.certain(5L)
  }

  test("lala2") {
    given Runtime with
      def platform: Platform = Platform.LinuxX64
    val clong = 5.asInstanceOf[fr.hammons.slinc.experimental.CLong]

    assertEquals(clong + clong, fr.hammons.slinc.experimental.CLong(5))
  }

  test("lala3") {
    given Runtime with
      def platform: Platform = Platform.LinuxX64

    summon[Runtime].platform match
      case given Platform.WinX64 =>
        fr.hammons.slinc.experimental.CLong.lesser(
          (1: Byte)
        ) + fr.hammons.slinc.experimental.CLong.lesser((2: Short))
      case given Platform.LinuxX64 =>
        fr.hammons.slinc.experimental.CLong.lesser(
          (1: Byte)
        ) + fr.hammons.slinc.experimental.CLong.lesser((2: Short))
      case Platform.MacX64 => ???

  }

  test("lala4") {
    given Runtime with
      def platform: Platform = Platform.LinuxX64

    // fr.hammons.slinc.experimental.CLong.minimaOrLess(5)
    assertEquals(
      fr.hammons.slinc.experimental.CLong.minimaOrLess((5: Short)),
      fr.hammons.slinc.experimental.CLong(5)
    )
  }

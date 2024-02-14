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
    for i <- 0 until 1000000000 do assertEquals(x.abs.toInt, 5)

    // lo.map(_.toString())

    // given Platform.LinuxX64.type = Platform.LinuxX64

    // ong.certain(5L)
  }

  test("lala2") {
    val x = -5L
    for i <- 0 until 1000000000 do assertEquals(x.abs.toInt, 5)
  }

package fr.hammons.slinc

import fr.hammons.slinc.internal.Describe
import fr.hammons.slinc.CNumericDefMin.given
import fr.hammons.slinc.internal.LightOption
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

    val lo: LightOption[CLong] =
      for
        i <- CLong(5L)
        j <- CLong(23L)
      yield i + j

    lo.map(_.toString())

    given Platform.LinuxX64.type = Platform.LinuxX64

    CLong.certain(5L)
  }

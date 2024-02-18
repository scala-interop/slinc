package fr.hammons.slinc.experimental

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import fr.hammons.slinc.Runtime
import fr.hammons.slinc.Platform
import org.scalacheck.Prop.*

class CLongSpec extends ScalaCheckSuite:
  val minimaGen: Gen[Byte | Short | Int] = Gen.oneOf(
    Arbitrary.arbitrary[Byte],
    Arbitrary.arbitrary[Int],
    Arbitrary.arbitrary[Short]
  )

  val runtimeGen =
    for _platform <- Gen
        .oneOf(Platform.LinuxX64, Platform.MacX64, Platform.WinX64)
    yield new Runtime:
      def platform: Platform = _platform

  property("can instantiate from minima or less") {
    forAll(minimaGen, runtimeGen) { (n, runtime) =>
      try
        given Runtime = runtime
        val o: Long = n match
          case b: Byte  => b
          case s: Short => s
          case i: Int   => i
        assertEquals(CLong.minimaOrLess(n).toIntegral, o)
      catch case e => fail("error", e)
    }
  }

  test("casting is caught") {
    given Runtime with
      def platform: Platform = Platform.LinuxX64

    intercept[Error] {
      val i = 5.asInstanceOf[CLong]
      i + i
    }
  }

  property("can instantiate from lesser") {
    forAll(runtimeGen, Arbitrary.arbitrary[Short]) { (runtime, short) =>
      val clong = runtime.platform match
        case given fr.hammons.slinc.Platform.WinX64   => CLong.lesser(short)
        case given fr.hammons.slinc.Platform.LinuxX64 => CLong.lesser(short)
        case given fr.hammons.slinc.Platform.MacX64   => CLong.lesser(short)

      assertEquals(clong.toInt(using runtime), short.toInt)
    }
  }

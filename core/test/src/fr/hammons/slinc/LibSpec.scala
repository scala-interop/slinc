package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import java.{util as ju}
import scala.annotation.nowarn
import fr.hammons.slinc.types.CLong

@nowarn("msg=unused import")
trait LibSpec(val slinc: Slinc) extends ScalaCheckSuite {
  import modules.LibModule

  import slinc.{CLong as _, given, *}

  trait GoodLib derives Lib:
    def abs(a: Int): Int

  trait BadFunctionName derives Lib:
    def babs(a: Int): Int

  import slinc.given LibModule
  property("good lib works") {
    val goodLib = Lib.instance[GoodLib]
    forAll { (i: Int) =>
      assertEquals(goodLib.abs(i), i.abs)
    }

  }

  test("bad function name lib fails") {
    intercept[ju.NoSuchElementException] {
      Lib.instance[BadFunctionName]
    }
  }

  test("generic binding support".ignore) {
    val error = compileErrors("""trait GoodLib2 derives Lib:
                       def qsort[A](
                          array: Ptr[A],
                          num: Int,
                          size: Int,
                          fn: Ptr[(Ptr[A], Ptr[A]) => Int]
                       ): Unit""")

    assertNoDiff(error, "")
  }

  test("variadic support") {
    val error = compileErrors("""
    trait VariadicLib derives Lib:
      def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Seq[Variadic]): Unit
    """)

    trait VariadicLib derives Lib:
      def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Seq[Variadic]): Unit

    val variadicLib = Lib.instance[VariadicLib]

    assertNoDiff(error, "")

    Scope.confined {
      val format = Ptr.copy("%i hello %s %i")
      val buffer = Ptr.blankArray[Byte](256)

      assertEquals(format.copyIntoString(200), "%i hello %s %i")
      variadicLib.sprintf(buffer, format, Seq(1, Ptr.copy("hello"), 2))
      assertEquals(buffer.copyIntoString(256), "1 hello hello 2")
    }
  }

  test("allocating returns") {
    import slinc.given
    case class div_t(quot: Int, rem: Int) derives Struct

    trait AllocatingLib derives Lib:
      def div(i: Int, r: Int): div_t

    val allocatingLib = Lib.instance[AllocatingLib]

    assertEquals(allocatingLib.div(5, 2), div_t(2, 1))
  }

  test("platform dependent types") {
    val maybeError = compileErrors("""
    trait PlatformLib derives Lib:
      def labs(long: CLong): CLong
    """)

    trait PlatformLib derives Lib:
      def labs(long: CLong): CLong

    assertNoDiff(maybeError, "")

    val platformLib = Lib.instance[PlatformLib]

    val input = CLong(-3)
    val expectedOutput = CLong(3)

    assertEquals(platformLib.labs(input), expectedOutput)
  }
}

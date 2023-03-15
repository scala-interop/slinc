package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import java.{util as ju}
import scala.annotation.nowarn

@nowarn("msg=unused import")
trait LibSpec(val slinc: Slinc) extends ScalaCheckSuite {
  import modules.LibModule

  trait GoodLib derives Lib:
    def abs(a: Int): Int

  trait BadFunctionName derives Lib:
    def babs(a: Int): Int

  import slinc.given LibModule
  property("good lib works") {
    forAll { (i: Int) =>
      assertEquals(Lib[GoodLib].abs(i), i.abs)
    }

  }

  test("bad function name lib fails") {
    intercept[ju.NoSuchElementException] {
      Lib[BadFunctionName]
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

  test("variadic support".ignore) {
    val error = compileErrors("""
    trait VariadicLib derives Lib:
      def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Variadic*): Unit
    """)

    assertNoDiff(error, "")
  }
}

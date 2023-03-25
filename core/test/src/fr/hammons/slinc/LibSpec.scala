package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import scala.annotation.nowarn
import fr.hammons.slinc.types.{CInt, CLong, TimeT, CChar}
import fr.hammons.slinc.types.{OS, Arch}
import fr.hammons.slinc.annotations.*

class LibSpec extends munit.FunSuite:
  test("Unit parameters don't compile"):
      val compileResult = compileErrors("""
    trait L derives Lib:
      def myFun(o: Unit): Unit
    """)

      val errorMessage = """error: No Descriptor for Unit"""
      assert(compileResult.nonEmpty)
      assertNoDiff(compileResult.split("\n").nn(0).nn, errorMessage)

  test("generic binding support".ignore):
      val error = compileErrors("""trait GoodLib2 derives Lib:
                       def qsort[A](
                          array: Ptr[A],
                          num: Int,
                          size: Int,
                          fn: Ptr[(Ptr[A], Ptr[A]) => Int]
                       ): Unit""")

      assertNoDiff(error, "")

  test("variadic support"):
      val error = compileErrors("""
    trait VariadicLib derives Lib:
      def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Seq[Variadic]): Unit
    """)

      assertNoDiff(error, "")

  test("platform dependent types"):
      val maybeError = compileErrors("""
    trait PlatformLib derives Lib:
      def labs(long: CLong): CLong
    """)

      assertNoDiff(maybeError, "")

  test("function descriptors should be sane"):
      trait TestLib derives Lib:
        def time(t: Ptr[TimeT]): TimeT
        def abs(c: CInt): Unit
        def sprintf(string: Ptr[CChar], args: Seq[Variadic]): Ptr[CChar]

      assertEquals(
        summon[Lib[TestLib]].description,
        List(
          new CFunctionDescriptor(
            Map.empty.withDefaultValue("time"),
            Seq(DescriptorOf[Ptr[TimeT]]),
            false,
            Some(DescriptorOf[TimeT])
          ),
          new CFunctionDescriptor(
            Map.empty.withDefaultValue("abs"),
            Seq(DescriptorOf[CInt]),
            false,
            None
          ),
          new CFunctionDescriptor(
            Map.empty.withDefaultValue("sprintf"),
            Seq(DescriptorOf[Ptr[CChar]]),
            true,
            Some(DescriptorOf[Ptr[CChar]])
          )
        )
      )

  test("name overrides should be recorded"):
      trait L derives Lib:
        @NameOverride("_time64", (OS.Windows, Arch.X64))
        def time(t: Int): Int

      assertEquals(
        summon[Lib[L]].description,
        List(
          new CFunctionDescriptor(
            Map((OS.Windows, Arch.X64) -> "_time64").withDefaultValue("time"),
            Seq(DescriptorOf[Int]),
            false,
            Some(DescriptorOf[Int])
          )
        )
      )

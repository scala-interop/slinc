package fr.hammons.slinc

import fr.hammons.slinc.types.{CInt, TimeT, CChar}
import fr.hammons.slinc.types.{OS, Arch}
import fr.hammons.slinc.annotations.*
import fr.hammons.slinc.fset.Dependency
import java.nio.file.Paths

class FSetSpec extends munit.FunSuite:
  test("Unit parameters don't compile"):
      val compileResult = compileErrors("""
    trait L derives FSet:
      def myFun(o: Unit): Unit
    """)

      val errorMessage = """error: No Descriptor for Unit"""
      assert(compileResult.nonEmpty)
      assertNoDiff(compileResult.split("\n").nn(0).nn, errorMessage)

  test("generic binding support".ignore):
      val error = compileErrors("""trait GoodLib2 derives FSet:
                       def qsort[A](
                          array: Ptr[A],
                          num: Int,
                          size: Int,
                          fn: Ptr[(Ptr[A], Ptr[A]) => Int]
                       ): Unit""")

      assertNoDiff(error, "")

  test("variadic support"):
      val error = compileErrors("""
    trait VariadicLib derives FSet:
      def sprintf(ret: Ptr[Byte], string: Ptr[Byte], args: Seq[Variadic]): Unit
    """)

      assertNoDiff(error, "")

  test("platform dependent types"):
      val maybeError = compileErrors("""
    import fr.hammons.slinc.types.CLong
    trait PlatformLib derives FSet:
      def labs(long: CLong): CLong
    """)

      assertNoDiff(maybeError, "")

  test("function descriptors should be sane"):
      trait TestLib derives FSet:
        def time(t: Ptr[TimeT]): TimeT
        def abs(c: CInt): Unit
        def sprintf(string: Ptr[CChar], args: Seq[Variadic]): Ptr[CChar]

      assertEquals(
        summon[FSet[TestLib]].description,
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
      trait L derives FSet:
        @NameOverride("_time64", (OS.Windows, Arch.X64))
        def time(t: Int): Int

      assertEquals(
        summon[FSet[L]].description,
        List(
          new CFunctionDescriptor(
            Map((OS.Windows, Arch.X64) -> "_time64").withDefaultValue("time"),
            Seq(DescriptorOf[Int]),
            false,
            Some(DescriptorOf[Int])
          )
        )
      )

  test("library resources should be recorded for loading"):
      @NeedsResource("test")
      trait L derives FSet:
        def abs(i: CInt): CInt

      assertEquals(
        summon[FSet[L]].dependencies,
        List(Dependency.LibraryResource(Paths.get("test").nn))
      )

  test("c resources should be recorded for loading"):
      @NeedsResource("test.c")
      trait L derives FSet:
        def abs(i: CInt): CInt

      assertEquals(
        summon[FSet[L]].dependencies,
        List(Dependency.CResource(Paths.get("test.c").nn))
      )

  test("library dependencies should be recorded for loading"):
      @Needs("posix")
      trait L derives FSet:
        def abs(i: CInt): CInt

      assertEquals(
        summon[FSet[L]].dependencies,
        List(Dependency.PathLibrary("posix"))
      )

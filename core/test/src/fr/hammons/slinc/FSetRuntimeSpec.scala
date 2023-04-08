package fr.hammons.slinc

import java.{util as ju}
import fr.hammons.slinc.annotations.NeedsResource
import fr.hammons.slinc.types.CInt
import fr.hammons.slinc.types.{os, OS}
import fr.hammons.slinc.annotations.NeedsFile
import java.nio.file.Files
import java.nio.file.Paths

trait FSetRuntimeSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.given

  test("bad function name lib fails"):
      trait L derives FSet:
        def babs(a: Int): Int

      intercept[ju.NoSuchElementException]:
          FSet.instance[L]

  test("return value necessitating allocation detection"):
      case class div_t(a: Int, b: Int) derives Struct
      trait L derives FSet:
        def div(i: Int, r: Int): div_t

      assert(
        FunctionContext(
          summon[FSet[L]].description.head
        ).returnAllocates
      )

  test("C resource loading works"):
      @NeedsResource("test.c")
      trait L derives FSet:
        def identity_int(i: CInt): CInt

      assertEquals(
        FSet.instance[L].identity_int(5),
        5
      )

  test("Lib resource absolute loading works"):
      if os == OS.Windows then
        @NeedsResource("test_x64.dll")
        trait L derives FSet:
          def identity_int(i: CInt): CInt

        assertEquals(
          FSet.instance[L].identity_int(2),
          2
        )
      else
        @NeedsResource("test_x64.so")
        trait L derives FSet:
          def identity_int(i: CInt): CInt

        assertEquals(
          FSet.instance[L].identity_int(2),
          2
        )

  test("Lib resource platform independent loading works"):
      @NeedsResource("test")
      trait L derives FSet:
        def identity_int(i: CInt): CInt

      assertEquals(
        FSet.instance[L].identity_int(2),
        2
      )

  test("Overridden relative file loading works"):
      if os == OS.Windows then
        @NeedsFile("libs/test.dll")
        trait L derives FSet:
          def test_fn(i: CInt): CInt

        assertEquals(
          FSet.instance[L].test_fn(2),
          2
        )
      else
        @NeedsFile("libs/test.so")
        trait L derives FSet:
          def test_fn(i: CInt): CInt

        assertEquals(
          FSet.instance[L].test_fn(2),
          2
        )

  test("Platform independent file loading works"):
      @NeedsFile("libs/test")
      trait L derives FSet:
        def test_fn(i: CInt): CInt

      assertEquals(
        FSet.instance[L].test_fn(2),
        2
      )

  test("Absolute file loading works"):
      if os == OS.Linux then
        if !Files.exists(Paths.get("/tmp/test.so")) then
          Files.copy(Paths.get("libs/test.so"), Paths.get("/tmp/test.so"))

        @NeedsFile("/tmp/test.so")
        trait L derives FSet:
          def test_fn(i: CInt): CInt

        assertEquals(
          FSet.instance[L].test_fn(2),
          2
        )
      else assert(true)

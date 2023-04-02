package fr.hammons.slinc

import java.{util as ju}
import fr.hammons.slinc.annotations.NeedsResource
import fr.hammons.slinc.types.CInt

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

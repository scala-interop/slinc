package fr.hammons.slinc

import java.{util as ju}

trait LibModuleSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.given

  test("bad function name lib fails"):
      trait L derives Lib:
        def babs(a: Int): Int

      intercept[ju.NoSuchElementException]:
          Lib.instance[L]

  test("return value necessitating allocation detection"):
      case class div_t(a: Int, b: Int) derives Struct
      trait L derives Lib:
        def div(i: Int, r: Int): div_t

      assert(
        CFunctionRuntimeInformation(
          summon[Lib[L]].description.head
        ).returnAllocates
      )

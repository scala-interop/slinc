package fr.hammons.slinc

import Slinc17.default.{given, *}
import fr.hammons.slinc.BindingsSpec.div_t

class BindingsSpec extends munit.FunSuite:
  import BindingsSpec.Cstd.*
  test("abs") {
    assertEquals(abs(4), 4)
  }

  test("div") {
    assertEquals(div(5,2), div_t(2,1))
  }

  //todo: LongLayout and other Layout supports
  // test("labs") {
  //   assertEquals(labs(-4l), 4l)
  // }


  test("rand") {
    assertNotEquals(rand(), rand())
  }

  //todo: upcall support needed.
  //test("qsort")

object BindingsSpec:
  case class div_t(a: Int, b: Int) derives Struct
  object Cstd derives Library:
    def abs(a: Int): Int = Library.binding
    def div(a: Int, b: Int): div_t = Library.binding
    def rand(): Int = Library.binding
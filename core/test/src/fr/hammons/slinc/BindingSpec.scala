package fr.hammons.slinc

import munit.ScalaCheckSuite

trait BindingSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given,*}
  @LibraryName("@test")
  object Test derives Library:
    def identity_int(i: CInt): CInt = Library.binding

  test("int_identity") {
    assertEquals(Test.identity_int(5), 5)
  }


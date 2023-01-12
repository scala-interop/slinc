package fr.hammons.slinc

import munit.ScalaCheckSuite

trait BindingSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given, *}
  @LibraryName("@test")
  object Test derives Library:
    def identity_int(i: CInt): CInt = Library.binding
    // issue 31 test binding
    def i31test(struct: I31Struct): Ptr[CChar] = Library.binding

    case class I31Struct(field: Ptr[CChar]) derives Struct

  test("int_identity") {
    assertEquals(Test.identity_int(5), 5)
  }

  test("issue 31 fix") {
    Scope.confined {
      val struct = Test.I31Struct(
        Ptr.copy("hello world!")
      )
      Test.i31test(struct)
    }
  }

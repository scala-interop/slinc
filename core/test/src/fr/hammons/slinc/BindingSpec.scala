package fr.hammons.slinc

import munit.ScalaCheckSuite
import scala.concurrent.duration.*

trait BindingSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given, *}
  override def munitTimeout: Duration = 5.minutes
  @LibraryName("@test")
  object Test derives Library:
    def identity_int(i: CInt): CInt = Library.binding

    case class I31Struct(field: Ptr[CChar]) derives Struct

    // issue 31 test binding
    def i31test(struct: I31Struct): Ptr[CChar] = Library.binding

    case class I36Struct(i: CInt, c: Ptr[CChar]) derives Struct

    case class I36Inner(i: CInt) derives Struct 
    case class I36Outer(inner: Ptr[I36Inner]) derives Struct

    // issue 36 test bindings
    def i36_get_my_struct(): Ptr[I36Struct] = Library.binding
    def i36_get_mystruct_by_value(): I36Struct  = Library.binding
    def i36_copy_my_struct(ptr: Ptr[I36Struct]): Unit = Library.binding
    def i36_nested(): Ptr[I36Outer] = Library.binding

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

  test("issue 36 fix") {
    val ptr = Test.i36_get_my_struct()

    assertEquals((!ptr).i, 42)
    assertEquals((!ptr).c.copyIntoString(100), "mylib")

    val struct = Test.i36_get_mystruct_by_value()

    assertEquals(struct.i, 42)
    assertEquals(struct.c.copyIntoString(100), "mylib")

    Scope.confined{
      !ptr = Test.I36Struct(21, Ptr.copy("test"))
      Test.i36_copy_my_struct(ptr)
    }

    assertEquals((!ptr).i, 42)
    assertEquals((!ptr).c.copyIntoString(100), "mylib")

    val ptr2 = Test.i36_nested()

    assertEquals((!(!ptr2).inner).i, 43) 
  }


package fr.hammons.slinc

import munit.ScalaCheckSuite
import scala.concurrent.duration.*
import annotations.Needs
import fr.hammons.slinc.annotations.NeedsResource
import fr.hammons.slinc.types.*

trait BindingSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given, *}
  override def munitTimeout: Duration = 5.minutes

  case class I31Struct(field: Ptr[CChar]) derives Struct
  case class I36Struct(i: CInt, c: Ptr[CChar]) derives Struct

  case class I36Inner(i: CInt) derives Struct
  case class I36Outer(inner: Ptr[I36Inner]) derives Struct

  @NeedsResource("test")
  trait TestLib derives FSet:
    def identity_int(i: CInt): CInt

    // issue 31 test binding
    def i31test(struct: I31Struct): Ptr[CChar]

    // issue 36 test bindings
    def i36_get_my_struct(): Ptr[I36Struct]
    def i36_get_mystruct_by_value(): I36Struct
    def i36_copy_my_struct(ptr: Ptr[I36Struct]): Unit
    def i36_nested(): Ptr[I36Outer]

  test("int_identity") {
    val test = FSet.instance[TestLib]

    assertEquals(test.identity_int(5), 5)
  }

  test("issue 31 fix") {
    val test = FSet.instance[TestLib]

    Scope.confined {
      val struct = I31Struct(
        Ptr.copy("hello world!")
      )
      test.i31test(struct)
    }
  }

  test("issue 36 fix") {
    val test = FSet.instance[TestLib]

    val ptr = test.i36_get_my_struct()

    assertEquals((!ptr).i, 42)
    assertEquals((!ptr).c.copyIntoString(100), "mylib")

    val struct = test.i36_get_mystruct_by_value()

    assertEquals(struct.i, 42)
    assertEquals(struct.c.copyIntoString(100), "mylib")

    Scope.confined {
      !ptr = I36Struct(21, Ptr.copy("test"))
      test.i36_copy_my_struct(ptr)
    }

    assertEquals((!ptr).i, 42)
    assertEquals((!ptr).c.copyIntoString(100), "mylib")

    val ptr2 = test.i36_nested()

    assertEquals((!(!ptr2).inner).i, 43)
  }

  // ZLIB test
  test("zlib works"):
      val test = FSet.instance[TestLib]

      @Needs("z")
      trait ZLib derives FSet:
        def zlibVersion(): Ptr[CChar]
      val zlib = FSet.instance[ZLib]

      val version = zlib.zlibVersion().copyIntoString(256)

      assertEquals(
        version.count(_ == '.'),
        2,
        s"$version contains more than 2 periods."
      )

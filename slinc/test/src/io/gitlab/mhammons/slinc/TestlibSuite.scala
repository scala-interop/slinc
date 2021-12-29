package io.gitlab.mhammons.slinc

class TestlibSuite extends munit.FunSuite {
   test("modify") {
      val a = Testlib.a_t(3, 4)
      val b = Testlib.b_t(5, a)

      assertEquals(
        scope(Testlib.slinc_test_modify(b)),
        b.copy(d = a.copy(a = 9))
      )
   }

   test("addone") {
      val c = Testlib.c_t(StaticArray[Int, 3], StaticArray[Float, 3])

      val result = scope(Testlib.slinc_test_addone(c))

      c.a.underlying
         .zip(result.a.underlying)
         .foreach((o, r) => assertEquals(r, o + 1))
      c.b.underlying
         .zip(result.b.underlying)
         .foreach((o, r) => assertEquals(r, o + 1))
   }

}

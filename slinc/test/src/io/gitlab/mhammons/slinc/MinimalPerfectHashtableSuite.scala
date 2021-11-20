package io.gitlab.mhammons.slinc

import components.MinimalPerfectHashtable

class MinimalPerfectHashtableSuite extends munit.FunSuite {

   test("can initialize from seq of literals") {
      val minimalPerfectHashtable =
         PerfectHashtable.runtimeConstruct(
           List("a", "b", "c"),
           List(1, 2, 3)
         )
   }

   test("returns correct values") {
      val minimalPerfectHashtable =
         PerfectHashtable.runtimeConstruct(
           List("tm_div", "tm_mday", "tm_year"),
           List(1, 2, 3)
         )

      println(minimalPerfectHashtable.values)
      assertEquals(minimalPerfectHashtable("tm_div"), 1)
      assertEquals(minimalPerfectHashtable("tm_mday"), 2)
      assertEquals(minimalPerfectHashtable("tm_year"), 3)
   }

   test("returns correct values") {
      val minimalPerfectHashtable =
         MinimalPerfectHashtable("tm_div", "tm_mday", "tm_year")(
           1,
           2,
           3
         )

      val minimalPerfectHashTable =
         MinimalPerfectHashtable("tm_div", "tm_mday", "tm_year")(1, 2, 3)

      assertEquals(minimalPerfectHashtable("tm_div"), 1)
      assertEquals(minimalPerfectHashtable("tm_mday"), 2)
      assertEquals(minimalPerfectHashtable("tm_year"), 3)
   }

}

package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.ResourceScope.globalScope
import jdk.incubator.foreign.SegmentAllocator

class SegmentSuite extends munit.FunSuite {
   given SegmentAllocator = SegmentAllocator.arenaAllocator(globalScope)
   // val i = int.allocate

   // test("can allocate int") {
   //    int.allocate
   // }

   // test("can apply and update int") {
   //    val i = int.allocate
   //    assertEquals(i(), 0)
   //    i() = 5
   //    assertEquals(i(), 5)
   // }

}

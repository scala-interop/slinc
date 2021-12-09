package io.gitlab.mhammons.slinc_benches

import io.gitlab.mhammons.slinc.*
import jdk.incubator.foreign.SegmentAllocator

case class a_t(a: Int, b: Int) derives Struct
case class b_t(c: Int, d: a_t) derives Struct

object LibTest
    extends Library(Location.Local("../../../../slinc/test/native/libtest.so")):

   def slinc_test_modify(b_t: Ptr[b_t])(using SegmentAllocator): Unit = bind

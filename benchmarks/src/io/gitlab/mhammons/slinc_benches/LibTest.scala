package io.gitlab.mhammons.slinc_benches

import io.gitlab.mhammons.slinc.*
import io.gitlab.mhammons.slinc.components.Library
import io.gitlab.mhammons.slinc.components.Location
import jdk.incubator.foreign.SegmentAllocator

case class a_t(a: Int, b: Int) derives Struckt
case class b_t(c: Int, d: a_t) derives Struckt

object LibTest
    extends Library(Location.Local("../../../../slinc/test/native/libtest.so")):

   def slinc_test_modify(b_t: b_t)(using SegmentAllocator): b_t = bind

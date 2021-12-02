package io.gitlab.mhammons.slinc

import io.gitlab.mhammons.slinc.components.Library
import io.gitlab.mhammons.slinc.components.Location

import jdk.incubator.foreign.SegmentAllocator

object Testlib extends Library(Location.Local("slinc/test/native/libtest.so")):
   type a_t = Struct {
      val a: int
      val b: int
   }

   type b_t = Struct {
      val c: int
      val d: a_t
   }

   def slinc_test_modify(b_t: b_t)(using SegmentAllocator): b_t = bind

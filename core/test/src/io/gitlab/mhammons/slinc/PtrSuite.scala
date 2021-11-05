package io.gitlab.mhammons.slinc

import io.gitlab.mhammons.slinc.*
import jdk.incubator.foreign.CLinker.C_POINTER

class PtrSuite extends munit.FunSuite:
   test("pointer goes to layout properly") {
      val result = ???
      assertEquals(result, C_POINTER)
   }

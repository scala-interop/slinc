package io.gitlab.mhammons.cstd

import munit.FunSuite
import io.gitlab.mhammons.slinc.*

class StdIOSuite extends FunSuite:
   import StdIO.*
   test("sprintf") {
      val result = scope {
         val buf = allocate[Byte](80)
         val format = "%d %d".serialize
         sprintf(buf, format)(5, 10)

         buf.mkString
      }
      assertEquals(result, "5 10")
   }

   test("sscanf") {
      val (aRes, bRes) = scope {
         val str = "5 10".serialize
         val format = "%d %d".serialize
         val a = allocate[Int](1)
         val b = allocate[Int](1)

         sscanf(str, format)(a, b)

         (!a, !b)
      }

      assertEquals(aRes, 5)
      assertEquals(bRes, 10)
   }

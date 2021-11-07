package io.gitlab.mhammons.slinc

import cats.implicits.*
import cats.catsInstancesForId

class StdlibSuite extends munit.FunSuite {

   test("getpid") {
      val getpid = NativeIO.function[() => Long]("getpid")

      assertEquals(
        getpid().foldMap(NativeIO.impureCompiler),
        ProcessHandle.current().pid
      )
   }

   test("getpie".fail) {
      NativeIO.function[() => Long]("getpie")().foldMap(NativeIO.impureCompiler)
   }
}

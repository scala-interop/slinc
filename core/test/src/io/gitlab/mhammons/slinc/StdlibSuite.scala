package io.gitlab.mhammons.slinc

import cats.implicits.*
import cats.catsInstancesForId

class StdlibSuite extends munit.FunSuite {
   val abs = NativeIO.function[Int => Int]("abs")
   val atof = NativeIO.function[String => Double]("atof")

   test("getpid") {
      val getpid = NativeIO.function[() => Long]("getpid")

      assertEquals(
        getpid().foldMap(NativeIO.impureCompiler),
        ProcessHandle.current().pid
      )
   }

   test("abs") {
      assertEquals(
        abs(5).foldMap(NativeIO.impureCompiler),
        Math.abs(5)
      )
   }

   test("atof") {
      assertEquals(
        NativeIO.scope(atof("5.0")).foldMap(NativeIO.impureCompiler),
        5.0
      )
   }

   test("getenv") {
      val getEnv = NativeIO.function[String => String]("getenv")
      assertEquals(
        NativeIO.scope(getEnv("PATH")).foldMap(NativeIO.impureCompiler),
        System.getenv("PATH")
      )
   }

   test("getpie".fail) {
      NativeIO.function[() => Long]("getpie")().foldMap(NativeIO.impureCompiler)
   }
}

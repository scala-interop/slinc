package io.gitlab.mhammons.slinc

import cats.implicits.*
import cats.catsInstancesForId

class StdlibSuite extends munit.FunSuite:
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

   test("labs") {
      val labs = NativeIO.function[Long => Long]("labs")
      assertEquals(
        labs(-5L).foldMap(NativeIO.impureCompiler),
        5L
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

   test("strlen") {
      val strlen = NativeIO.function[String => Int]("strlen")
      assertEquals(
        NativeIO.scope(strlen("hello")).foldMap(NativeIO.impureCompiler),
        5
      )
   }

   test("div") {
      import Fd.int
      type div_t = Struct {
         val quot: int
         val rem: int
      }
      val div = NativeIO.function[(Int, Int) => div_t]("div")
      NativeIO
         .scope(
           for
              res <- div(5, 2)
              quot = res.quot.get
              rem = res.rem.get
           yield
              assertEquals(quot, 2)
              assertEquals(rem, 1)
         )
         .compile(NativeIO.impureCompiler)
   }

   test("getpie".fail) {
      NativeIO.function[() => Long]("getpie")().foldMap(NativeIO.impureCompiler)
   }

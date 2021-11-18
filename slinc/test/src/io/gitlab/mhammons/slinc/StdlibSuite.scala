package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.SegmentAllocator

class StdlibSuite extends munit.FunSuite:
   given NativeCache = NativeCacheDefaultImpl()

   test("getpid") {
      def getpid: Long = bind

      assertEquals(
        getpid,
        ProcessHandle.current().pid
      )
   }

   test("abs") {
      def abs(i: Int): Int = bind
      assertEquals(
        abs(5),
        Math.abs(5)
      )
   }

   test("labs") {
      def labs(l: Long): Long = bind
      assertEquals(
        labs(-5L),
        5L
      )
   }

   test("atof") {
      def atof(s: String)(using NativeCache, SegmentAllocator): Double = bind
      assertEquals(
        scope(atof("5.0")),
        5.0
      )
   }

   test("getenv") {
      def getenv(name: String)(using NativeCache, SegmentAllocator): String =
         bind
      assertEquals(
        scope(getenv("PATH")),
        System.getenv("PATH")
      )
   }

   test("strlen") {
      def strlen(string: String)(using NativeCache, SegmentAllocator): Int =
         bind
      assertEquals(
        scope(strlen("hello")),
        5
      )
   }

   test("div") {
      import Member.int
      type div_t = Struct {
         val quot: int
         val rem: int
      }
      def div(num: Int, denom: Int)(using SegmentAllocator): div_t = bind
      scope {
         val result = div(5, 2)
         assertEquals(result.quot(), 2)
         assertEquals(result.rem(), 1)
      }
   }

// test("getpie".fail) {
//    NativeIO.function[() => Long]("getpie")().foldMap(NativeIO.impureCompiler)
// }

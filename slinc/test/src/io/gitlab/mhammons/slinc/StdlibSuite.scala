package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.SegmentAllocator

class StdlibSuite extends munit.FunSuite:
   case class tm(
       tm_sec: Int,
       tm_min: Int,
       tm_hour: Int,
       tm_mday: Int,
       tm_mon: Int,
       tm_year: Int,
       tm_wday: Int,
       tm_yday: Int,
       tm_isdst: Int
   ) derives Struct

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
      def atof(s: String)(using SegmentAllocator): Double = bind
      assertEquals(
        scope(atof("5.0")),
        5.0
      )
   }

   test("getenv") {
      def getenv(name: String)(using SegmentAllocator): String =
         bind
      assertEquals(
        scope(getenv("PATH")),
        System.getenv("PATH")
      )
   }

   test("strlen") {
      def strlen(string: String)(using SegmentAllocator): Int =
         bind
      assertEquals(
        scope(strlen("hello")),
        5
      )
   }

   test("div") {
      case class div_t(quot: Int, rem: Int) derives Struct

      def div(a: Int, b: Int)(using SegmentAllocator): div_t = bind

      val result = scope {
         div(5, 2)
      }
      assertEquals(result, div_t(2, 1))
   }

   test("asctime") {

      def time(timer: Ptr[Long]): Long = bind

      val t = time(Ptr.Null())

      def localtime(timer: Ptr[Long]): Ptr[tm] = bind

      def asctime(timePtr: Ptr[tm])(using SegmentAllocator): String = bind

      val result = scope {
         val timerPtr = t.serialize
         val tmPtr = localtime(timerPtr)
         asctime(tmPtr)
      }
   }

   test("localtime") {
      def time(timer: Ptr[Long]): Long = bind

      def localtime(timer: Ptr[Long]): Ptr[tm] = bind
   }

// test("getpie".fail) {
//    NativeIO.function[() => Long]("getpie")().foldMap(NativeIO.impureCompiler)
// }

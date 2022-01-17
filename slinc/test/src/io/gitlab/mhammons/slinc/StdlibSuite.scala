package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.SegmentAllocator
import components.layoutOf

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
      def atof(s: String): Double = bind
      assertEquals(
        scope(atof("5.0")),
        5.0
      )
   }

   test("getenv") {
      def getenv(name: String): String =
         bind
      assertEquals(
        scope(getenv("PATH")),
        System.getenv("PATH")
      )
   }

   test("strlen") {
      def strlen(string: String): Int =
         bind
      assertEquals(
        scope(strlen("hello")),
        5
      )
   }

   test("div") {
      case class div_t(quot: Int, rem: Int) derives Struct

      def div(a: Int, b: Int): div_t = bind

      val result = scope {
         div(5, 2)
      }
      assertEquals(result, div_t(2, 1))
   }

   test("asctime") {
      import platform.time_t

      def time(timer: Ptr[Long]): time_t = bind

      val t = time(Ptr.Null())

      val y = t + t

      def localtime(timer: Ptr[time_t]): Ptr[tm] = bind

      def asctime(timePtr: Ptr[tm]): String = bind

      val result = scope {
         val timerPtr = t.serialize
         val tmPtr = localtime(timerPtr)
         asctime(tmPtr)
      }
   }

   test("localtime") {
      def time(timer: Ptr[Long]): platform.time_t = bind

      def localtime(timer: Ptr[Long]): Ptr[tm] = bind
   }

   test("qsort") {
      def qsort(
          arr: Ptr[Any],
          numElements: Long,
          elementSize: Long,
          fn: Ptr[(Ptr[Any], Ptr[Any]) => Int]
      ): Unit = bind

      val arr = Array(2, 1, 5, 8, -1)
      val result = scope {
         val ptr = Array(2, 1, 5, 8, -1).serialize.castTo[Any]
         val fn =
            (a1: Ptr[Any], a2: Ptr[Any]) => !a1.castTo[Int] - !a2.castTo[Int]
         qsort(
           ptr,
           arr.size,
           layoutOf[Int].byteSize(),
           fn.serialize
         )

         ptr.castTo[Int].toArray(5)
      }

      assertEquals(result.toSeq, arr.sorted.toSeq)
   }

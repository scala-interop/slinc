package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.SegmentAllocator
import components.{Ptr, NPtr}

class StdlibSuite extends munit.FunSuite:
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
      case class div_t(quot: Int, rem: Int) derives Struckt

      def div(a: Int, b: Int)(using SegmentAllocator): div_t = bind

      val result = scope {
         div(5, 2)
      }
      assertEquals(result, div_t(2, 1))
   }

// test("div") {
//    type div_t = Struct {
//       val quot: int
//       val rem: int
//    }
//    import io.gitlab.mhammons.slinc.components.LayoutOf
//    summon[LayoutOf[div_t]]
//    def div(num: Int, denom: Int)(using SegmentAllocator): div_t = bind
//    scope {
//       val result = div(5, 2)
//       assertEquals(result.quot(), 2)
//       assertEquals(result.rem(), 1)
//    }
// }

// test("asctime"){
//    type tm= Struct{
//       val tm_sec: int
//       val tm_min: int
//       val tm_hour: int
//       val tm_mday: int
//       val tm_mon: int
//       val tm_year: int
//       val tm_wday: int
//       val tm_yday: int
//       val tm_isdst: int
//    }

//    import components.BoundaryCrossing

//    summon[BoundaryCrossing[Ptr[long], ?]]

//    def time(timer: Ptr[long]): long = bind

//    val t = time(Ptr.nul)

//    def asctime(timePtr: Ptr[tm])(using SegmentAllocator): String = bind

//    asctime(~t)

// }

// test("getpie".fail) {
//    NativeIO.function[() => Long]("getpie")().foldMap(NativeIO.impureCompiler)
// }

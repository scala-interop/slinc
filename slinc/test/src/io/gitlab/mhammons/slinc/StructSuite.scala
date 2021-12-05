package io.gitlab.mhammons.slinc

import io.gitlab.mhammons.slinc.components.StructLayout
import io.gitlab.mhammons.slinc.components.{Primitives, Named, Template}
import scala.collection.immutable.ArraySeq
import jdk.incubator.foreign.SegmentAllocator
import java.io.File
import java.nio.file.Paths

class StructSuite extends munit.FunSuite:
   test(
     "trying to fetch the layouts for c-incompatible types results in compiletime error".fail
   ) {
      assertNoDiff(
        compileErrors(
          "NativeIO.layout[Object].foldMap(NativeIO.impureCompiler)"
        ),
        """|error: Cannot derive a layout for non-struct type java.lang.Object
           |        compileErrors(
           |                    ^
           |""".stripMargin
      )
   }

   test(
     "Can serialize and deserialize simple structs"
   ) {
      case class div_t(quot: Int, rem: Int) derives Struckt

      val result = scope {
         !div_t(5, 2).serialize
      }

      assertEquals(result, div_t(5, 2))
   }

   test("can serialize and deserialize nested structs") {
      case class div_t(quot: Int, rem: Int) derives Struckt
      case class div_x(x: Int, div: div_t) derives Struckt

      val sample = div_x(5, div_t(5, 4))

      val result = scope {
         !sample.serialize
      }

      assertEquals(sample, result)
   }

   test("can update structs that are on the native heap") {
      case class div_t(quot: Int, rem: Int) derives Struckt
      val expectedResult = div_t(2, 9)
      val result = scope {
         val ptr = div_t(3, 4).serialize
         !ptr = expectedResult
         !ptr
      }
      assertEquals(result, expectedResult)
   }

   test("can partially dereference pointers") {
      case class div_t(quot: Int, rem: Int) derives Struckt
      val result = scope {
         !div_t(3, 4).serialize.partial.quot
      }

      assertEquals(3, result)
   }

   test("can partially update pointers") {
      case class div_t(quot: Int, rem: Int) derives Struckt
      val result = scope {
         val ptr = div_t(3, 4).serialize
         !ptr.partial.quot = 8
         !ptr
      }

      assertEquals(result, div_t(8, 4))
   }

   //  test(
   //    "can retrieve layouts for simple structs made of non-pointer primitives"
   //  ) {

   //     type div_t = Struct {
   //        val quot: int
   //        val rem: int
   //     }
   //     assertEquals(
   //       LayoutMacros.deriveLayout2[div_t],
   //       StructLayout(
   //         Seq(Named(Primitives.Int, "quot"), Named(Primitives.Int, "rem"))
   //       )
   //     )
   //  }

   //  test(
   //    "can retrieve layouts for structs with structs, and product types"
   //  ) {
   //     type div_x = Struct {
   //        val quot: int
   //        val rem: int
   //     }

   //     type div_y = Struct {
   //        val j: int
   //        val div_x: div_x
   //     }

   //     summon[Template[div_y]]

   //     assertEquals(
   //       LayoutMacros.deriveLayout2[div_y],
   //       StructLayout(
   //         Seq(
   //           Named(Primitives.Int, "j"),
   //           Named(
   //             StructLayout(
   //               Seq(Named(Primitives.Int, "quot"), Named(Primitives.Int, "rem"))
   //             ),
   //             "div_x"
   //           )
   //         )
   //       )
   //     )
   //  }

   test(
     "produces compile-time errors for structs that contain non-field members".fail
   ) {
      type div_t = Struct {
         val quot: Int
         val rem: Int
      }
      assertNoDiff(
        compileErrors(
          "type div_t = Struct{ val quot: Int; val rem: Int};NativeIO.layout[div_t].compile(NativeIO.impureCompiler)"
        ),
        ""
      )
   }

//  test("can allocate and use nested structs") {

//     scope {
//        val b_t = allocate[Testlib.b_t]
//        b_t.d.a() = 6
//        val b2 = Testlib.slinc_test_modify(b_t)

//        assertEquals(b_t.d.a(), 6)
//        assertEquals(b2.d.a(), 12)
//     }

//     case class X(a: Int, b: Int)

//  }

// test(
//   "nested structs have memsegment addresses that are in line with what's expected"
// ) {
//    scope {
//       val b_t = allocate[Testlib.b_t]
//       val offsetOfA_t =
//          summon[NativeCache].layout[Testlib.b_t].byteOffset("d")
//       assertEquals(
//         b_t.d.$mem.address.toRawLongValue - b_t.$mem.address.toRawLongValue,
//         offsetOfA_t
//       )
//    }
// }

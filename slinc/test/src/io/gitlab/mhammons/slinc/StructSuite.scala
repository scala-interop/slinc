package io.gitlab.mhammons.slinc

import io.gitlab.mhammons.slinc.components.StructLayout
import io.gitlab.mhammons.slinc.components.{Primitives, Named}
import scala.collection.immutable.ArraySeq
import jdk.incubator.foreign.SegmentAllocator
import java.io.File
import java.nio.file.Paths

class StructSuite extends munit.FunSuite:
   Testlib
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
     "can retrieve layouts for simple structs made of non-pointer primitives"
   ) {
      import components.Member.int
      type div_t = Struct {
         val quot: int
         val rem: int
      }
      assertEquals(
        summon[NativeCache].layout[div_t],
        StructLayout(
          Seq(Named(Primitives.Int, "quot"), Named(Primitives.Int, "rem"))
        )
      )
   }

   test(
     "can retrieve layouts for structs with structs, and product types"
   ) {
      import components.Member.int
      type div_x = Struct {
         val quot: int
         val rem: int
      }

      type div_y = Struct {
         val j: int
         val div_x: div_x
      }

      assertEquals(
        summon[NativeCache].layout[div_y],
        StructLayout(
          Seq(
            Named(Primitives.Int, "j"),
            Named(
              StructLayout(
                Seq(Named(Primitives.Int, "quot"), Named(Primitives.Int, "rem"))
              ),
              "div_x"
            )
          )
        )
      )
   }

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

   test("can grab varhandles from a struct layout") {
      type div_t = Struct {
         val quot: Int
         val rem: Int
      }

      val varhandles = summon[NativeCache].varHandles[div_t]
   }

   test("test") {}

   test("can allocate and use nested structs") {

      scope {
         val b_t = allocate[Testlib.b_t]()
         b_t.d.a() = 6
         val b2 = Testlib.slinc_test_modify(b_t)

         assertEquals(b_t.d.a(), 6)
         assertEquals(b2.d.a(), 12)
      }

   }

   test(
     "nested structs have memsegment addresses that are in line with what's expected"
   ) {
      scope {
         val b_t = allocate[Testlib.b_t]()
         val offsetOfA_t =
            summon[NativeCache].layout[Testlib.b_t].byteOffset("d")
         assertEquals(
           b_t.d.$mem.address.toRawLongValue - b_t.$mem.address.toRawLongValue,
           offsetOfA_t
         )
      }
   }

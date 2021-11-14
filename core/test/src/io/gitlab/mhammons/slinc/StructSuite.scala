package io.gitlab.mhammons.slinc

import cats.catsInstancesForId
import NativeCache.given NativeCache
import io.gitlab.mhammons.slinc.components.StructLayout
import io.gitlab.mhammons.slinc.components.Primitives

class StructSuite extends munit.FunSuite:
   given NativeCache = NativeCache()
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
      import Fd.int
      type div_t = Struct {
         val quot: int
         val rem: int
      }
      assertEquals(
        summon[NativeCache].layout2[div_t],
        StructLayout("quot" -> Primitives.Int, "rem" -> Primitives.Int)
      )
   }

   test(
     "can retrieve layouts for structs with structs, and product types"
   ) {
      import Fd.int
      type div_x = Struct {
         val quot: int
         val rem: int
      }

      type div_y = Struct {
         val j: int
         val div_x: div_x
      }

      assertEquals(
        summon[NativeCache].layout2[div_y],
        StructLayout(
          "j" -> Primitives.Int,
          "div_x" -> StructLayout(
            "quot" -> Primitives.Int,
            "rem" -> Primitives.Int
          )
        )
      )
   }

   test(
     "produces compile-time errors for structs that contain non-field members"
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

package io.gitlab.mhammons.slinc

import cats.catsInstancesForId

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
     "can retrieve layouts for simple structs made of non-pointer primitives"
   ) {
      import Fd.int
      type div_t = Struct {
         val quot: int
         val rem: int
      }
      NativeIO.layout[div_t].compile(NativeIO.impureCompiler)
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

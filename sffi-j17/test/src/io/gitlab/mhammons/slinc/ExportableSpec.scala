package fr.hammons.sffi

import ffi.*
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.CLinker.*

class ExportableSpec extends munit.FunSuite {
  val size = MemoryLayout.unionLayout(C_INT, C_LONG_LONG, C_CHAR).nn.byteSize()
  println(size)
  test("can export integers") {
    Scope(){
      val ptr = toNative(5)
      assertEquals(!ptr, 5)
      //ptr.`unary_!_=`(4)
      ptr.update(4)
      assertEquals(!ptr, 4)
    }
  }

  test("can allocate strings") {
    Scope(){
      val ptr = toNative("hello world!")

      assertEquals(String(ptr.toArray("hello world!".size)), "hello world!")
    }
  }

  //case class div_t(quot: Int, rem: Int) derives Struct
  // test("can export structs") {
  //   Scope(){
  //     val ptr = toNative(div_t(2,1))

  //     assertEquals(!ptr, div_t(2,1))
  //   }
  // }
}

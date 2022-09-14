package fr.hammons.sffi

import ffi.*
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.*
import jdk.incubator.foreign.CLinker.*



class ExportableSpec extends munit.FunSuite {
  test("ffi2 can export structs") {
    val ffi3 = FFI173
    import ffi3.*
    case class X(a: Int, b: Int)

    println(layoutOf[X])
    val size = MemoryLayout.structLayout(C_INT, C_INT).nn.byteSize()
    val mem = 
      CLinker.allocateMemory(size).nn.asSegment(size, ResourceScope.globalScope()).nn

    write(mem.asInstanceOf[basics.RawMem], 0, X(1,2))

  }

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

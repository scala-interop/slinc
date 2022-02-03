package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.SegmentAllocator
import components.{Cache, SymbolLookup, NonNative, FromNative}
import scala.annotation.tailrec
object Testlib
    extends AbsoluteLocation
    with Library(Location.Absolute(BuildInfo.libtest)) derives CLibrary:
   def path = BuildInfo.libtest
   case class a_t(a: Int, b: Int) derives Struct
   case class b_t(c: Int, d: a_t) derives Struct

   case class d_t(p: Ptr[a_t]) derives Struct
   case class c_t(a: StaticArray[Int, 3], b: StaticArray[Float, 3])
       derives Struct

   def slinc_test_modify(b_t: b_t) = accessNative[b_t]
   def slinc_test_addone(c_t: c_t) = bind[c_t]
   def slinc_test_getstaticarr(): Ptr[Int] = bind[Ptr[Int]]
   def slinc_test_passstaticarr(res: Ptr[Int]): Unit = bind[Unit]
   def slinc_two_structs(a: a_t, b: a_t): Int = bind[Int]
   def slinc_upcall(zptr: Ptr[() => Int]): Int = bind[Int]
   def slinc_upcall_a_t(zptr: Ptr[() => a_t]): Int = bind[Int]
   def slinc_fptr_ret(): Ptr[() => a_t] = bind[Ptr[() => a_t]]
   def slinc_fptr_ret2(): Ptr[(Int, Int) => Int] = bind[Ptr[(Int, Int) => Int]]

   def byte_test(b: Byte): Byte = bind[Byte]
   def short_test(a: Short): Short = bind[Short]
   def int_test(a: Int): Int = bind[Int]
   def long_test(a: Long): Long = bind[Long]
   def char_test(a: AsciiChar): AsciiChar = bind[AsciiChar]
   def string_test(str: Ptr[Byte]): AsciiChar = bind[AsciiChar]
   def bool_test(a: Boolean): Boolean = bind[Boolean]
   def float_test(f: Float): Float = bind[Float]
   def double_test(d: Double): Double = bind[Double]
   def sum(n: Int) = accessNativeVariadic[Int](n)

   def bad_method(str: Ptr[Byte]): Unit = bind[Unit]
   def ibreak(str: Ptr[Byte]): String = bind[String]
object TestLib2:
   import Testlib.given SymbolLookup

   def sum(n: Int) = variadicBind[Int](n)

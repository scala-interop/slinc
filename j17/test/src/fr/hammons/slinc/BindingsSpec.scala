package fr.hammons.slinc

import Slinc17.default.{given, *}
import fr.hammons.slinc.BindingsSpec.div_t
import scala.util.Random

class BindingsSpec extends munit.FunSuite:
  import BindingsSpec.Cstd.*
  test("abs") {
    assertEquals(abs(4), 4)
  }

  test("div") {
    assertEquals(div(5,2), div_t(2,1))
  }

  //todo: LongLayout and other Layout supports
  // test("labs") {
  //   assertEquals(labs(-4l), 4l)
  // }


  test("rand") {
    assertNotEquals(rand(), rand())
  }

  test("qsort") {
    val testArray = Random.shuffle(Array.fill(1024)(Random.nextInt().toShort.toInt)).toArray
    Scope.confined{
      val arr = Ptr.copy(testArray)

      qsort[Int](arr, testArray.size, 4, Ptr.upcall((a,b) => 
        val aVal = !a
        val bVal = !b 
        if aVal < bVal then -1 
        else if aVal == bVal then 0 
        else 1
      ))

      assertEquals(arr.asArray(testArray.size).toSeq, testArray.sorted.toSeq)
    }
  }
object BindingsSpec:
  case class div_t(a: Int, b: Int) derives Struct
  object Cstd derives Library:
    def abs(a: Int): Int = Library.binding
    def div(a: Int, b: Int): div_t = Library.binding
    def rand(): Int = Library.binding
    def qsort[A](array: Ptr[A], num: Long, size: Long, fn: Ptr[(Ptr[A], Ptr[A]) => A]): Unit = Library.binding
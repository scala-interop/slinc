package fr.hammons.slinc

import fr.hammons.slinc.BindingsSpec.div_t
import scala.util.Random

trait BindingsSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.{given, *}

  object Cstd derives Library:
    def abs(a: Int): Int = Library.binding
    def div(a: Int, b: Int): div_t = Library.binding
    def rand(): Int = Library.binding
    def qsort[A](array: Ptr[A], num: Long, size: Long, fn: Ptr[(Ptr[A], Ptr[A]) => Int]): Unit = Library.binding

  given Struct[div_t] = Struct.derived

  test("abs") {
    assertEquals(Cstd.abs(4), 4)
  }

  test("div") {
    assertEquals(Cstd.div(5,2), div_t(2,1))
  }

  test("rand") {
    assertNotEquals(Cstd.rand(), Cstd.rand())
  }

  test("qsort") {
    val testArray = Random.shuffle(Array.fill(1024)(Random.nextInt())).toArray
    Scope.confined{
      val arr = Ptr.copy(testArray)

      Cstd.qsort(arr, testArray.size, 4, Ptr.upcall((a,b) => 
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
  case class div_t(a: Int, b: Int)
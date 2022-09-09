package fr.hammons.sffi

import scala.util.Random

trait BindingSpec(val f: FFI) extends munit.FunSuite:
  import f.*
  val abs = fnGen[Int => Int]("abs")
  val labs = fnGen[Long => Long]("labs")
  val srand = fnGen[Int => Unit]("srand")
  case class div_t(quot: Int, rem: Int) derives Struct
  val div = fnGen[(Int, Int) => div_t]("div")
  val sprintf = fnGen[(Ptr[Byte], Ptr[Byte], Seq[Variadic]) => Int]("sprintf")
  val qsort =
    fnGen[(Ptr[Any], Long, Long, Ptr[(Ptr[Any], Ptr[Any]) => Int]) => Unit](
      "qsort"
    )

  def qsortMeth[A](
      array: Ptr[A],
      elemSize: Long,
      elements: Long,
      fn: Ptr[(Ptr[A], Ptr[A]) => Int]
  ) =
    qsort(array.as[Any], elemSize, elements, fn.as[(Ptr[Any], Ptr[Any]) => Int])
  // val v = fnGen[(Int,Float,Int) => Int]("abos")

  // t.call()

  test("abs") {
    assertEquals(
      srand(-5),
      ()
    )
  }

  test("labs") {
    assertEquals(
      labs(-5L),
      5L
    )
  }

  test("srand") {
    assertEquals(
      srand(3),
      ()
    )
  }

  test("sprintf") {
    Scope() {
      val res = Ptr.blank[Byte](256)
      sprintf(res, toNative("hello %s %d"), Seq(toNative("world"), 420))
      assertEquals(
        res.asString,
        "hello world 420"
      )
    }
  }

  test("div") {
    val res = div(5, 2)

    assertEquals(res, div_t(2, 1))
  }

  test("qsort") {
    Scope() {
      val nums = Random.shuffle((0 until 1024).toArray)

      val in = toNative(nums.toArray)

      qsortMeth(
        in,
        4.toLong,
        1024.toLong,
        toNative { (a, b) =>
          {
            if !a > !b then 1
            else if !a == !b then 0
            else -1
          }
        }
      )
    }
  }

package fr.hammons.slinc

class SetSizeArraySpec extends munit.FunSuite:
  test("instantiate"):
      val result = compileErrors("SetSizeArray(1,2,3)")
      assertNoDiff(result, "")

  test("fromArray"):
      {
        val result = compileErrors("SetSizeArray.fromArray[-1](Array(1,2,3))")
        val expected =
          """|error: Cannot prove that (0 : Int) <= (-1 : Int) =:= (true : Boolean).
            |SetSizeArray.fromArray[-1](Array(1,2,3))
            |                         ^
            |""".stripMargin
        assertNoDiff(result, expected)
      }
      {
        val result = compileErrors("SetSizeArray.fromArray[Int](Array(1,2,3))")
        val expected =
          """|error: Cannot prove that (0 : Int) <= Int =:= (true : Boolean).
                 |SetSizeArray.fromArray[Int](Array(1,2,3))
                 |                          ^
                 |""".stripMargin
        assertNoDiff(result, expected)
      }

      assert(SetSizeArray.fromArray[5](Array(1, 2)).isEmpty)
      assert(SetSizeArray.fromArray[5](Array(1, 2, 3, 4, 5, 6)).isEmpty)
      assert(SetSizeArray.fromArray[5](Array(1, 2, 3, 4, 5)).isDefined)

  test("fromArrayUnsafe"):
      val result =
        compileErrors("SetSizeArray.fromArrayUnsafe[Int](Array(1,2))")
      val expected =
        """|error: Cannot prove that (0 : Int) <= Int =:= (true : Boolean).
             |SetSizeArray.fromArrayUnsafe[Int](Array(1,2))
             |                                ^
             |""".stripMargin
      assertNoDiff(result, expected)

  test("apply"):
      val result = compileErrors("SetSizeArray(1,2,3)[3]")
      val expected =
        """|error: Cannot prove that (3 : Int) < (3 : Int) =:= (true : Boolean).
           |SetSizeArray(1,2,3)[3]
           |                     ^""".stripMargin
      assertNoDiff(result, expected)

      assertEquals(SetSizeArray(1, 2, 3)[1], 2)

  test("put"):
      val result = compileErrors("SetSizeArray(1,2,3).put[4](4)")
      val expected =
        """|error: Cannot prove that (4 : Int) < (3 : Int) =:= (true : Boolean).
         |SetSizeArray(1,2,3).put[4](4)
         |                            ^""".stripMargin

      assertNoDiff(result, expected)

      val arr = SetSizeArray.ofDim[3, Int]
      arr.put[1](4)
      assertEquals(arr[1], 4)

  test("isEqual"):
      assert(SetSizeArray(1, 2, 3).isEqual(SetSizeArray(1, 2, 3)))
      assert(!SetSizeArray(1, 2, 3).isEqual(SetSizeArray(2, 4, 6)))

  test("map"):
      assert(SetSizeArray(1, 2, 3).map(_ * 2).isEqual(SetSizeArray(2, 4, 6)))

  test("flatmap"):
      assert(
        SetSizeArray(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          .flatMap(_ => SetSizeArray.ofDim[0, Int])
          .isEqual(SetSizeArray.ofDim[0, Int])
      )

      assert(
        SetSizeArray(1, 2, 3)
          .flatMap(v => SetSizeArray(v - 1, v - 2, v - 3))
          .isEqual(
            SetSizeArray(
              0, -1, -2, 1, 0, -1, 2, 1, 0
            )
          )
      )

  test("concat"):
      val a = SetSizeArray(1, 2)
      val b = SetSizeArray(3, 4)
      assert(
        a.concat(b).isEqual(SetSizeArray(1, 2, 3, 4))
      )

      val result =
        compileErrors("SetSizeArray(1,2,3).concat(SetSizeArray('4','5'))")
      val expected = """|error: Cannot prove that (0 : Int) <= Int + (3 : Int) =:= (true : Boolean).
                        |SetSizeArray(1,2,3).concat(SetSizeArray('4','5'))
                        |                                                ^
                        |error:
                        |Found:    fr.hammons.slinc.SetSizeArray[Char, (2 : Int)]
                        |Required: fr.hammons.slinc.SetSizeArray[Int, Int]
                        |
                        |One of the following imports might make progress towards fixing the problem:
                        |
                        |  import fr.hammons.slinc.container.Container.getContainer
                        |  import munit.Clue.generate
                        |
                        |SetSizeArray(1,2,3).concat(SetSizeArray('4','5'))
                        |                          ^""".stripMargin
      assertNoDiff(result, expected)

  test("drop"):
      {
        val result = compileErrors("SetSizeArray(1,2,3).drop[4]")
        val expected =
          """|error: Cannot prove that (0 : Int) <= (3 : Int) - (4 : Int) =:= (true : Boolean).
             |SetSizeArray(1,2,3).drop[4]
             |                          ^""".stripMargin

        assertNoDiff(result, expected)
      }

      {
        val result = compileErrors("SetSizeArray(1,2,3).drop[-1]")
        val expected =
          """|error: Cannot prove that (0 : Int) <= (-1 : Int) =:= (true : Boolean).
             |SetSizeArray(1,2,3).drop[-1]
             |                           ^""".stripMargin

        assertNoDiff(result, expected)
      }

      assert(
        SetSizeArray(1, 2, 3).drop[1].isEqual(SetSizeArray(2, 3))
      )

  test("take"):
      assert(
        SetSizeArray(1, 2, 3).take[2].isEqual(SetSizeArray(1, 2))
      )

  test("forall"):
      assertEquals(SetSizeArray(1, 2, 3).forall(_ > 0), true)

  test("exists"):
      assertEquals(SetSizeArray(1, 2, 3).exists(_ > 2), true)

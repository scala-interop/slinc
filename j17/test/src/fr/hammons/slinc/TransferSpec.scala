package fr.hammons.slinc


class TransferSpec extends munit.FunSuite:
  test("can read and write jvm ints") {
    import Slinc17.default.{*, given}
    Allocator.global{
      val mem = Ptr.blank[Int]

      !mem = 6
      assertEquals(!mem, 6)
    }
  }


  test("can read and write simple structs") {
    import Slinc17.default.{*, given}
    case class X(a: Int, b: Int) derives Struct
    Allocator.global{
      val mem = Ptr.blank[X]

      !mem = X(2,3)
      assertEquals(!mem, X(2,3))
    }
  }

  test("can read and write complex structs") {
    import Slinc17.default.{*, given}
    case class Y(a: Int, b: Int) derives Struct 
    case class X(a: Int, y: Y, b: Int) derives Struct 

    Allocator.global{
      val mem = Ptr.blank[X]

      !mem = X(2,Y(2,3),3)
      assertEquals(!mem, X(2,Y(2,3),3))
    }
  }

  test("compiled reader and writer works") {
    import Slinc17.immediateJit.{*, given}
    case class Y(a: Int, b: Int) derives Struct 
    case class X(a: Int, y: Y, b: Int) derives Struct 

    Allocator.global{
      val mem = Ptr.blank[X]

      !mem = X(2,Y(2,3),3)
      assertEquals(!mem, X(2,Y(2,3),3))
    }
  }

  test("top-level compiled reader and writer works") {
    import TransferSpec.*
    import Slinc17.immediateJit.{*, given}
    given Struct[A] = Struct.derived
    given Struct[B] = Struct.derived

    Allocator.global{
      val mem = Ptr.blank[B]
      val testValue = B(2,A(2,3),3)
      !mem = testValue
      assertEquals(!mem, testValue)
    }
  }
object TransferSpec:
  case class A(a: Int, b: Int) 
  case class B(a: Int, b: A, c: Int)

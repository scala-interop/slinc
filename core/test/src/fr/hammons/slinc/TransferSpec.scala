package fr.hammons.slinc

object TransferSpec:
  case class A(a: Int, b: Int)
  case class B(a: Int, b: A, c: Int)

trait TransferSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.{*, given}

  given Struct[TransferSpec.A] = Struct.derived
  given Struct[TransferSpec.B] = Struct.derived
  
  test("can read and write jvm ints") {
    Scope.global{
      val mem = Ptr.blank[Int]

      !mem = 6

      assertEquals(!mem, 6)
    }
  }

  test("can read and write simple local/inner structs") {
    case class X(a: SizeT, b: Int) derives Struct 
    Scope.global{
      val mem = Ptr.blank[X]

      !mem = X(2.toSizeT,3)
      assertEquals(!mem, X(2.toSizeT,3))
    }
  }

  test("can read and write complex local/inner structs") {
    case class Y(a: Int, b: Int) derives Struct 
    case class X(a: Int, y: Y, b: Int) derives Struct 

    Scope.global{
      val mem = Ptr.blank[X]
      
      !mem = X(2,Y(2,3), 3)
      assertEquals(!mem, X(2,Y(2,3),3))
    }
  }

  test("can read and write simple top-level structs") {
    import TransferSpec.*
    Scope.global{
      val mem = Ptr.blank[A]

      !mem = A(2,3)
      assertEquals(!mem, A(2,3))
    }
  }

  test("can read and write complex top-level structs") {
    import TransferSpec.*
    Scope.global{
      val mem = Ptr.blank[B]

      !mem = B(2,A(2,3),3)
      assertEquals(!mem, B(2,A(2,3),3))
    }
  }

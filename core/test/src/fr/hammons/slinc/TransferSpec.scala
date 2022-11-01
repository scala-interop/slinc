package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

object TransferSpec:
  case class A(a: Int, b: Int)
  case class B(a: Int, b: A, c: Int)
  case class C(a: Int, b: Byte, c: Short, d: Long, e: Float, f: Double)

trait TransferSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{*, given}

  given Struct[TransferSpec.A] = Struct.derived
  given Struct[TransferSpec.B] = Struct.derived
  given Struct[TransferSpec.C] = Struct.derived

  test("can read and write jvm ints") {
    Scope.global {
      val mem = Ptr.blank[Int]

      !mem = 6

      assertEquals(!mem, 6)
    }
  }

  inline def canReadWrite[A](using
      Arbitrary[A],
      Send[A],
      Receive[A],
      LayoutOf[A]
  ) = property(s"can read/write ${nameOf[A]}") {
    forAll { (b: A) =>
      Scope.confined {
        val mem = Ptr.blank[A]

        !mem = b
        assertEquals(!mem, b)
      }
    }
  }

  canReadWrite[Byte]
  canReadWrite[Int]
  canReadWrite[Float]
  canReadWrite[Double]
  canReadWrite[Long]
  canReadWrite[Short]

  test("can read and write simple local/inner structs") {
    case class X(a: SizeT, b: Int) derives Struct
    Scope.global {
      val mem = Ptr.blank[X]

      !mem = X(2.as[SizeT], 3)
      assertEquals(!mem, X(2.as[SizeT], 3))
    }
  }

  test("can read and write complex local/inner structs") {
    case class Y(a: Int, b: Int) derives Struct
    case class X(a: Int, y: Y, b: Int) derives Struct

    Scope.global {
      val mem = Ptr.blank[X]

      !mem = X(2, Y(2, 3), 3)
      assertEquals(!mem, X(2, Y(2, 3), 3))
    }
  }

  test("can read and write simple top-level structs") {
    import TransferSpec.*
    Scope.global {
      val mem = Ptr.blank[A]

      !mem = A(2, 3)
      assertEquals(!mem, A(2, 3))
    }
  }

  test("can read and write complex top-level structs") {
    import TransferSpec.*
    Scope.global {
      val mem = Ptr.blank[B]

      !mem = B(2, A(2, 3), 3)
      assertEquals(!mem, B(2, A(2, 3), 3))
    }
  }

  property("can read/write structs with all primitive types") {
    forAll { (a: Int, b: Byte, c: Short, d: Long, e: Float, f: Double) =>
      Scope.confined {

        val ts = TransferSpec.C(a, b, c, d, e, f)

        val mem = Ptr.blank[TransferSpec.C]
        !mem = ts
        assertEquals(!mem, ts)
      }
    }

  }

  property("can read function pointers") {
    forAll { (fn: Int => Int, i: Int) =>
      Scope.confined {
        val mem = Ptr.upcall(fn)
        assertEquals((!mem)(i), fn(i))
      }
    }
  }

  property("can read/write arrays of Int") {
    forAll { (arr: Array[Int]) =>
      Scope.confined {
        val mem = Ptr.copy(arr)
        assertEquals(mem.asArray(arr.length).toSeq, arr.toSeq)
      }
    }
  }

  val genA = for
    a <- Arbitrary.arbitrary[Int]
    b <- Arbitrary.arbitrary[Int]
  yield TransferSpec.A(a, b)

  property("can read/write arrays of A") {
    forAll(Gen.containerOf[Array, TransferSpec.A](genA)) {
      (arr: Array[TransferSpec.A]) =>
        Scope.confined {
          val mem = Ptr.copy(arr)

          assertEquals(mem.asArray(arr.length).toSeq, arr.toSeq)
        }
    }
  }

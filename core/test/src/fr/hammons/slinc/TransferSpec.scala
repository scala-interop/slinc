package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

trait TransferSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{*, given}

  case class A(a: Int, b: Int) derives Struct
  case class B(a: Int, b: A, c: Int) derives Struct 
  case class C(a: Int, b: Byte, c: Short, d: Long, e: Float, f: Double) derives Struct 
  case class D(a: Byte, b: Ptr[Int], c: Byte) derives Struct


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
    Scope.global {
      val mem = Ptr.blank[A]

      !mem = A(2, 3)
      assertEquals(!mem, A(2, 3))
    }
  }

  test("can read and write complex top-level structs") {
    Scope.global {
      val mem = Ptr.blank[B]

      !mem = B(2, A(2, 3), 3)
      assertEquals(!mem, B(2, A(2, 3), 3))
    }
  }

  property("can read/write structs with all primitive types") {
    forAll { (a: Int, b: Byte, c: Short, d: Long, e: Float, f: Double) =>
      Scope.confined {

        val ts = C(a, b, c, d, e, f)

        val mem = Ptr.blank[C]
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
  yield A(a, b)

  property("can read/write arrays of A") {
    forAll(Gen.containerOf[Array, A](genA)) {
      (arr: Array[A]) =>
        Scope.confined {
          val mem = Ptr.copy(arr)

          assertEquals(mem.asArray(arr.length).toSeq, arr.toSeq)
        }
    }
  }



  property("can read/write structs with pointers") {
    forAll { (a: Byte, b: Int, c: Byte) => 
      Scope.confined{
        val d = D(a, Ptr.copy(b), c)
        val ptr = Ptr.copy(d)
        val dPrime = !ptr

        assertEquals(d.a, dPrime.a)
        assertEquals(d.c, dPrime.c)
        assertEquals(!d.b, !dPrime.b)
      }
    }
  }

  

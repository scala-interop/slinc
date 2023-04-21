package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import types.*

trait TransferSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{*, given}

  val numVarArgs = if slinc.version < 19 then 7 else 200

  case class A(a: Int, b: Int) derives Struct
  case class B(a: Int, b: A, c: Int) derives Struct
  case class C(a: Int, b: Byte, c: Short, d: Long, e: Float, f: Double)
      derives Struct
  case class D(a: Byte, b: Ptr[Int], c: Byte) derives Struct

  case class E(list: VarArgs) derives Struct

  test("can read and write jvm ints") {
    Scope.global {
      val mem = Ptr.blank[Int]

      !mem = 6

      assertEquals(!mem, 6)
    }
  }

  inline def canReadWrite[A](using
      Arbitrary[A],
      DescriptorOf[A]
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

      !mem = X(SizeT(2.toShort), 3)
      assertEquals(!mem, X(SizeT(2.toShort), 3))
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
    forAll(Gen.containerOf[Array, A](genA)) { (arr: Array[A]) =>
      Scope.confined {
        val mem = Ptr.copy(arr)

        assertEquals(mem.asArray(arr.length).toSeq, arr.toSeq)
      }
    }
  }

  property("can read/write structs with pointers") {
    forAll { (a: Byte, b: Int, c: Byte) =>
      Scope.confined {
        val d = D(a, Ptr.copy(b), c)
        val ptr = Ptr.copy(d)
        val dPrime = !ptr

        assertEquals(d.a, dPrime.a)
        assertEquals(d.c, dPrime.c)
        assertEquals(!d.b, !dPrime.b)
      }
    }
  }

  test("varargs can be sent and retrieved"):
      Scope.confined {
        val vaListForVaList = VarArgsBuilder(4).build
        val vaList = VarArgsBuilder(
          4.toByte,
          5.toShort,
          6,
          7.toLong,
          2f,
          3d,
          Null[Int],
          A(1, 2),
          CLong(4: Byte),
          vaListForVaList
        ).build

        assertEquals(vaList.get[Byte], 4.toByte, "byte assert")
        assertEquals(vaList.get[Short], 5.toShort, "short assert")
        assertEquals(vaList.get[Int], 6, "int assert")
        assertEquals(vaList.get[Long], 7L, "long assert")
        assertEquals(vaList.get[Float], 2f, "float assert")
        assertEquals(vaList.get[Double], 3d, "double assert")
        assertEquals(
          vaList.get[Ptr[Int]].mem.asAddress,
          Null[Int].mem.asAddress,
          "ptr assert"
        )
        assertEquals(vaList.get[A], A(1, 2), "struct assert")
        assertEquals(vaList.get[CLong], CLong(4: Byte), "alias assert")
        assertEquals(vaList.get[VarArgs].get[CInt], 4)
      }

  test("varargs can be skipped"):
      Scope.confined {
        val vaList = VarArgsBuilder(
          4.toByte,
          2f
        ).build

        vaList.skip[Byte]
        assertEquals(vaList.get[Float], 2f)
      }

  test("varargs can be copied and reread"):
      Scope.confined {
        val vaList = VarArgsBuilder(
          4: Byte,
          2f
        ).build

        val vaList2 = vaList.copy()

        assertEquals(vaList.get[Byte], 4: Byte)
        assertEquals(vaList.get[Float], 2f)
        assertEquals(vaList2.get[Byte], 4: Byte)
        assertEquals(vaList2.get[Float], 2f)
      }

  test("varargs can be converted to and from pointers"):
      Scope.confined {
        val vaPtr = Ptr.copy(
          VarArgsBuilder(
            4: Byte,
            2f
          ).build
        )

        val vaList = !vaPtr

        assertEquals(vaList.get[Byte], 4: Byte)
        assertEquals(vaList.get[Float], 2f)
      }

  property("varargs can be embedded in structs"):
      forAll(Gen.listOfN(numVarArgs, Arbitrary.arbitrary[CInt])):
          (ints: Seq[CInt]) =>
            Scope.confined:
              val va = VarArgsBuilder
                .fromIterable(
                  ints.map(a => a: Variadic)
                )
                .build

              val x = va.copy()

              val p = Ptr.copy(E(x))

              val vaList = (!p).list

              ints.foreach: value =>
                assertEquals(va.get[CInt], value, "conversion test")

              ints.foreach: value =>
                assertEquals(vaList.get[CInt], value)

  property("pointers are equal as long as they point to the same memory"):
      forAll: (i: CInt) =>
        Scope.confined:
          val p1 = Ptr.copy(i)
          val p2 = p1.castTo[Float]

          assert(p1 == p2)
          assert(p1 != p1(2))
          assert(p1 == p1(0))

package fr.hammons.slinc

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import types.*
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag
import scala.util.chaining.*

trait TransferSpec[ThreadException <: Throwable](val slinc: Slinc)(using
    ClassTag[ThreadException]
) extends ScalaCheckSuite:
  import slinc.{*, given}
  System.setProperty("slinc.jitc.mode", "disabled")

  val numVarArgs = if slinc.version < 19 then 7 else 200

  case class A(a: Int, b: Int) derives Struct
  case class B(a: Int, b: A, c: Int) derives Struct
  case class C(a: Int, b: Byte, c: Short, d: Long, e: Float, f: Double)
      derives Struct
  case class D(a: Byte, b: Ptr[Int], c: Byte) derives Struct

  case class E(list: VarArgs) derives Struct

  case class F(u: CUnion[(CInt, CFloat)]) derives Struct

  case class G(long: CLong, arr: SetSizeArray[CLong, 2]) derives Struct

  case class H(a: Int, b: Float, c: CLong) derives Struct

  Scope.confined:
    Ptr.copy(H(1, 2, CLong(3)))

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
        val returnedFn = mem.unary_!
        assertEquals(returnedFn(i), fn(i))
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

  test("varargs can receive primitive types"):
      Scope.confined {
        val vaList = VarArgsBuilder(
          4.toByte,
          5.toShort,
          6,
          7.toLong,
          2f,
          3d,
          Null[Int]
        ).build

        assertEquals(vaList.get[Byte], 4.toByte, "byte assert")
        assertEquals(vaList.get[Short], 5.toShort, "short assert")
        assertEquals(vaList.get[Int], 6, "int assert")
        assertEquals(vaList.get[Long], 7L, "long assert")
        assertEquals(vaList.get[Float], 2f, "float assert")
        assertEquals(vaList.get[Double], 3d, "double assert")
        assertEquals(
          vaList.get[Ptr[Int]],
          Null[Int],
          "ptr assert"
        )
      }

  test("varargs can receive complex types".ignore):
      Scope.confined {
        val vaListForVaList = VarArgsBuilder(4).build
        val vaList = VarArgsBuilder(
          A(1, 2),
          CLong(4),
          A(3, 4),
          SetSizeArray(1, 2, 3, 4),
          vaListForVaList,
          CUnion[(CInt, CFloat)].tap(_.set(5)),
          // Null[Int],
          A(3, 4)
        ).build

        assertEquals(vaList.get[A], A(1, 2), "struct assert")
        assertEquals(vaList.get[CLong], CLong(4: Byte), "alias assert")
        assertEquals(vaList.get[A], A(3, 4))
        assertEquals(
          vaList.get[SetSizeArray[CInt, 4]].toSeq,
          Seq(1, 2, 3, 4),
          "set size array assert"
        )
        assertEquals(
          vaListForVaList.get[VarArgs].get[Int],
          4
        )
        assertEquals(
          vaList.get[CUnion[(CLongLong, CFloat)]].get[CLongLong],
          5L,
          "cunion assert"
        )
        // assertEquals(
        //   vaList.get[Ptr[Int]],
        //   Null[Int]
        // )
        assertEquals(
          vaList.get[A],
          A(3, 4),
          "struct assert 2"
        )
      }

  test("varargs can skip primitive types"):
      Scope.confined {
        val vaList = VarArgsBuilder(
          4: Byte,
          5: Short,
          6,
          7L,
          2f,
          3d,
          Null[Int]
        ).build

        val vaList2 = vaList.copy()

        vaList.skip[Byte]
        assertEquals(vaList.get[Short], 5: Short)
        vaList.skip[Int]
        assertEquals(vaList.get[Long], 7L)
        vaList.skip[Float]
        assertEquals(vaList.get[Double], 3d)
        vaList.skip[Ptr[Int]]

        assertEquals(vaList2.get[Byte], 4: Byte)
        vaList2.skip[Short]
        assertEquals(vaList2.get[Int], 6)
        vaList2.skip[Long]
        assertEquals(vaList2.get[Float], 2f)
        vaList2.skip[Double]
        assertEquals(vaList2.get[Ptr[Int]], Null[Int])
      }

  test("varargs can skip complex types".ignore):
      Scope.confined {
        val vaListForVaList = VarArgsBuilder(4, 5, 6).build
        val vaList = VarArgsBuilder(
          A(1, 2),
          CLong(4),
          vaListForVaList,
          CUnion[(CInt, CFloat)].tap(_.set(5)),
          SetSizeArray(1, 2, 3, 4)
        ).build

        val vaList2 = vaList.copy()

        assertEquals(vaList.get[A], A(1, 2), "struct assert")
        vaList.skip[CLong]
        val vaList3 = vaList.get[VarArgs]
        assertEquals(
          List(vaList3.get[Int], vaList3.get[Int], vaList3.get[Int]),
          List(4, 5, 6),
          "varargs assert"
        )
        vaList.skip[CUnion[(CInt, CFloat)]]
        assertEquals(
          vaList.get[SetSizeArray[Int, 4]].toSeq,
          Seq(1, 2, 3, 4),
          "set size array assert"
        )

        vaList2.skip[A]
        assertEquals(vaList2.get[CLong], CLong(4))
        vaList2.skip[VarArgs]
        assertEquals(vaList2.get[CUnion[(CInt, CFloat)]].get[Int], 5)
        vaList2.skip[SetSizeArray[Int, 4]]
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

  test("confined scope doesn't allow mem sharing"):
      Scope.confined {
        val ptr = Ptr.copy(5)
        intercept[ThreadException](
          Await.result(Future(!ptr), Duration.Inf)
        )
      }

  test("shared scope does allow mem sharing"):
      Scope.shared {
        val ptr = Ptr.copy(5)
        val res = Await.result(Future(!ptr), Duration.Inf)
        assertEquals(res, 5)
      }

  val randomA = for
    a <- Arbitrary.arbitrary[CInt]
    b <- Arbitrary.arbitrary[CInt]
  yield A(a, b)
  val randomUnion = Gen.oneOf[CInt | CFloat | A](
    Arbitrary.arbitrary[CInt],
    Arbitrary.arbitrary[CFloat],
    randomA
  )

  property("Can send and receive data from a Union"):
      val union = CUnion[(CInt, CFloat, A)]
      forAll(randomUnion):
          case a: CInt =>
            union.set(a)
            assertEquals(a, union.get[CInt])
          case a: CFloat =>
            union.set(a)
            assertEquals(a, union.get[CFloat])
          case a: A =>
            union.set(a)
            assertEquals(a, union.get[A])

  test("Unions deallocate when no-longer reachable"):
      var deallocated = false
      Scope.inferred { alloc ?=>
        alloc.addCloseAction(() => deallocated = true)
        CUnion[Tuple1[CInt]]
      }

      var tries = 5
      while tries > 0 && !deallocated do
        System.gc()
        Thread.sleep(100)
        tries -= 1

      assertEquals(deallocated, true)

  test("Unions are not deallocated early"):
      var deallocated = false
      val union = Scope.inferred { alloc ?=>
        alloc.addCloseAction(() => deallocated = true)
        CUnion[Tuple1[CInt]]
      }

      var tries = 5
      while tries > 0 && deallocated == false do
        System.gc()
        Thread.sleep(100)
        tries -= 1

      assertEquals(deallocated, false)
      assertEquals(union.get[CInt], 0)

  property("can create F ptrs"):
      val union = CUnion[(CInt, CFloat)]
      forAll: (a: CInt, b: CFloat, left: Boolean) =>
        if left then
          union.set(a)
          val fReturn = Scope.confined {
            val f = Ptr.copy(F(union))
            !f
          }

          assertEquals(fReturn.u.get[CInt], union.get[CInt])
        else
          union.set(b)
          val fReturn = Scope.confined {
            val f = Ptr.copy(F(union))
            !f
          }

          assertEquals(fReturn.u.get[CFloat], union.get[CFloat])

  test("can copy SetSizeArray[Int, 2] to native memory"):
      val ssa = SetSizeArray(1, 2)

      Scope.confined {
        val ptr = Ptr.copy(ssa)
        assertEquals((!ptr)[0], 1)
      }

  test("can copy SetSizeArray[CLong, 2] to native memory"):
      val ssa = SetSizeArray(CLong(1), CLong(2))

      Scope.confined {
        val ptr = Ptr.copy(ssa)
        assertEquals((!ptr)[0], CLong(1))
      }

  test("can copy G to native memory and back"):
      val g = G(CLong(5), SetSizeArray(CLong(1), CLong(2)))

      Scope.confined {
        val ptr = Ptr.copy(g)
        assertEquals((!ptr).arr[0], CLong(1))
      }

package fr.hammons.slinc

import munit.ScalaCheckSuite
import scala.concurrent.duration.*
import fr.hammons.slinc.annotations.NeedsResource
import fr.hammons.slinc.types.*
import org.scalacheck.Prop.*
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

trait BindingSpec(val slinc: Slinc) extends ScalaCheckSuite:
  import slinc.{given, *}

  val numArgumentsVArgs = if slinc.version == 17 then 7 else 200
  val numVariadic = 50
  override def munitTimeout: Duration = 5.minutes

  case class I31Struct(field: Ptr[CChar]) derives Struct
  case class I36Struct(i: CInt, c: Ptr[CChar]) derives Struct

  case class I36Inner(i: CInt) derives Struct
  case class I36Outer(inner: Ptr[I36Inner]) derives Struct

  case class I30Struct(list: Ptr[VarArgs]) derives Struct
  case class I175_Struct(union: CUnion[(CInt, CDouble)]) derives Struct

  @NeedsResource("test")
  trait TestLib derives FSet:
    def identity_int(i: CInt): CInt

    // issue 31 test binding
    def i31test(struct: I31Struct): Ptr[CChar]

    // issue 36 test bindings
    def i36_get_my_struct(): Ptr[I36Struct]
    def i36_get_mystruct_by_value(): I36Struct
    def i36_copy_my_struct(ptr: Ptr[I36Struct]): Unit
    def i36_nested(): Ptr[I36Outer]

    def i30_pass_va_list(count: CInt, args: VarArgs): CInt
    def i30_interspersed_ints_and_longs_va_list(
        count: CInt,
        args: VarArgs
    ): CLongLong
    def i30_function_ptr_va_list(
        count: CInt,
        fn: Ptr[(CInt, VarArgs) => CInt],
        args: Seq[Variadic]
    ): CInt
    def i157_null_eq(): Ptr[Unit]

    def i176_test(
        input: CUnion[(CFloat, CInt)],
        is_left: CChar
    ): CUnion[(CLong, CDouble)]

    def i175_test(
        input: I175_Struct,
        left: CChar
    ): I175_Struct

    def i180_test(
        input: SetSizeArray[CInt, 5]
    ): SetSizeArray[CInt, 5]

    def i213_test(b: CBool): CBool

  test("int_identity") {
    val test = FSet.instance[TestLib]

    assertEquals(test.identity_int(5), 5)
  }

  test("issue 31 fix") {
    val test = FSet.instance[TestLib]

    Scope.confined {
      val struct = I31Struct(
        Ptr.copy("hello world!")
      )
      test.i31test(struct)
    }
  }

  test("issue 36 fix") {
    val test = FSet.instance[TestLib]

    val ptr = test.i36_get_my_struct()

    assertEquals((!ptr).i, 42)
    assertEquals((!ptr).c.copyIntoString(100), "mylib")

    val struct = test.i36_get_mystruct_by_value()

    assertEquals(struct.i, 42)
    assertEquals(struct.c.copyIntoString(100), "mylib")

    Scope.confined {
      !ptr = I36Struct(21, Ptr.copy("test"))
      test.i36_copy_my_struct(ptr)
    }

    assertEquals((!ptr).i, 42)
    assertEquals((!ptr).c.copyIntoString(100), "mylib")

    val ptr2 = test.i36_nested()

    assertEquals((!(!ptr2).inner).i, 43)
  }

  property("issue 30 method argument test"):
    forAll(Gen.listOfN(numArgumentsVArgs, Arbitrary.arbitrary[CInt])):
      (ints: List[CInt]) =>
        val test = FSet.instance[TestLib]

        val result = Scope.confined {
          val vaList =
            VarArgsBuilder.fromIterable(ints.map(Variadic.apply(_))).build

          test.i30_pass_va_list(ints.size, vaList)
        }

        assertEquals(result, ints.sum)

  property("issue 30 int and long intersperse"):
    forAll(Gen.listOfN(numArgumentsVArgs, Arbitrary.arbitrary[CInt])):
      (ints: Seq[CInt]) =>
        val test = FSet.instance[TestLib]

        val result = Scope.confined {
          val variadics = ints.zipWithIndex.map((v, i) =>
            if i % 2 != 0 then Variadic(v.toLong) else Variadic(v)
          )
          val vaList = VarArgsBuilder.fromIterable(variadics).build

          test.i30_interspersed_ints_and_longs_va_list(
            variadics.size,
            vaList
          )
        }

        assertEquals(result, ints.map(_.toLong).sum)

  property("issue 30 function pointer test"):
    val fn = Scope.global {
      Ptr.upcall((count: CInt, vaList: VarArgs) =>
        (0 until count).map(_ => vaList.get[CInt]).sum
      )
    }
    forAll(Gen.listOfN(numVariadic, Arbitrary.arbitrary[CInt])):
      (args: Seq[CInt]) =>
        val test = FSet.instance[TestLib]

        val res = test.i30_function_ptr_va_list(
          args.size,
          fn,
          args.map(a => a: Variadic)
        )
        assertEquals(res, args.sum)

  test("Null is null"):
    val test = FSet.instance[TestLib]
    assertEquals(Null[Unit], test.i157_null_eq())

  property("issue 176 - can use and recieve union types from C functions"):
    val test = FSet.instance[TestLib]
    forAll: (float: CFloat, int: CInt, left: Boolean) =>
      val union = CUnion[(Float, Int)]
      if left then
        union.set(float)
        val res = test.i176_test(union, 1).get[CDouble]
        assertEquals(res, float.toDouble)
      else
        union.set(int)
        val res = test.i176_test(union, 0).get[CLong]
        assertEquals(res, CLong(int))

  property(
    "issue 175 - can send and receive structs with union types to C functions"
  ):
    val test = FSet.instance[TestLib]
    forAll: (int: CInt, double: CDouble, left: Boolean) =>
      val union = CUnion[(CInt, CDouble)]
      if left then
        union.set(int)
        val res = test.i175_test(I175_Struct(union), 1)
        assertEquals(
          res.union.get[CInt],
          int * 2
        )
      else
        union.set(double)
        val res = test.i175_test(I175_Struct(union), 0)
        assertEquals(res.union.get[CDouble], double / 2)

  property("issue 180 - can send and receive set size arrays to C functions"):
    val test = FSet.instance[TestLib]
    forAll(Gen.listOfN(5, Arbitrary.arbitrary[CInt])): list =>
      val arr = SetSizeArray.fromArrayUnsafe[5](list.toArray)
      val retArr = test.i180_test(arr)

      retArr.zip(arr.map(_ * 2)).foreach(assertEquals(_, _))

  property("issue 213 - can send and receive boolean values"):
    val test = FSet.instance[TestLib]
    forAll: (b: Boolean) =>
      assertEquals(test.i213_test(b), b)

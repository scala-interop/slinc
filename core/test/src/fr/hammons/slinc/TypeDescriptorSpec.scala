package fr.hammons.slinc
import types.*

class TypeDescriptorSpec extends munit.FunSuite:
  case class A(a: CInt, b: CInt) derives Struct
  test("DescriptorOf[CUnion[(Int, Float, A)]] gives appropriate descriptor"):
      assertEquals(
        DescriptorOf[CUnion[(CInt, CFloat, A)]]: TypeDescriptor,
        CUnionDescriptor(
          Set(IntDescriptor, FloatDescriptor, summon[Struct[A]].descriptor)
        ): TypeDescriptor
      )

  test(
    "DescriptorOf[CUnion[(CUnion[(Int, Float)], CUnion[(Int, Float)], A,A])]] doesn't double descriptors"
  ):
      assertEquals(
        DescriptorOf[
          CUnion[(CUnion[(Int, Float)], CUnion[(Int, Float)], A, A)]
        ]: TypeDescriptor,
        CUnionDescriptor(
          Set(
            CUnionDescriptor(Set(IntDescriptor, FloatDescriptor)),
            summon[Struct[A]].descriptor
          )
        ): TypeDescriptor
      )

  test("Transforms work in place of DescriptorOfs"):
      val result = compileErrors(
        """
        import fr.hammons.slinc.types.*
        
        trait Test derives FSet{
        def test(a: CBool): CBool
        def test2(a: CBoolShort): CBoolShort
        }

        val runtime: Slinc = ???

        import runtime.{*,given}
        val test = FSet.instance[Test]

        val bool: Boolean = test.test(true)
        val bool2: Boolean = test.test2(true)
      """
      )

      assertNoDiff(result, "")

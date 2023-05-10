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

package fr.hammons.slinc.modules

import fr.hammons.slinc.Slinc
import fr.hammons.slinc.DescriptorOf
import fr.hammons.slinc.types.CLongLong
import fr.hammons.slinc.types.CInt
import fr.hammons.slinc.types.CFloat
import fr.hammons.slinc.CUnion
import fr.hammons.slinc.Struct
import fr.hammons.slinc.ByteDescriptor
import fr.hammons.slinc.Bytes
import fr.hammons.slinc.ShortDescriptor
import fr.hammons.slinc.IntDescriptor
import fr.hammons.slinc.LongDescriptor
import fr.hammons.slinc.FloatDescriptor
import fr.hammons.slinc.DoubleDescriptor

trait DescriptorSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.dm
  case class A(a: CInt, b: CInt, c: CInt, d: CLongLong, e: CLongLong)
      derives Struct
  test("CUnionDescriptor.size gives the right size"):
      assertEquals(
        DescriptorOf[CUnion[(CInt, A, CFloat)]].size,
        DescriptorOf[A].size
      )

  test("CUnionDescriptor.alignment gives the right alignment"):
      assertEquals(
        DescriptorOf[CUnion[(CInt, A, CFloat)]].alignment,
        DescriptorOf[A].alignment
      )

  test("ByteDescriptor is 1 byte in size"):
      assertEquals(ByteDescriptor.size, Bytes(1))

  test("ShortDescriptor is 2 bytes in size"):
      assertEquals(ShortDescriptor.size, Bytes(2))

  test("IntDescriptor is 4 bytes in size"):
      assertEquals(IntDescriptor.size, Bytes(4))

  test("LongDescriptor is 8 bytes in size"):
      assertEquals(LongDescriptor.size, Bytes(8))

  test("FloatDescriptor is 4 bytes in size"):
      assertEquals(FloatDescriptor.size, Bytes(4))

  test("DoubleDescriptor is 8 bytes in size"):
      assertEquals(DoubleDescriptor.size, Bytes(8))

  test("StructDescriptor.alignment is the max of the member elements"):
      assertEquals(DescriptorOf[A].alignment, Bytes(8))

  test("StructDescriptor.size is a multiple of alignment"):

      assertEquals(DescriptorOf[A].size % DescriptorOf[A].alignment, Bytes(0))
      assert(
        DescriptorOf[A].size >= (DescriptorOf[CInt].size * 3 + DescriptorOf[
          CLongLong
        ].size * 2)
      )

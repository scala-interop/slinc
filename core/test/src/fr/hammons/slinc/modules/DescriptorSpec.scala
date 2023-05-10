package fr.hammons.slinc.modules

import fr.hammons.slinc.Slinc
import fr.hammons.slinc.DescriptorOf
import fr.hammons.slinc.types.CLongLong
import fr.hammons.slinc.types.CInt
import fr.hammons.slinc.types.CFloat
import fr.hammons.slinc.CUnion
import fr.hammons.slinc.Struct

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

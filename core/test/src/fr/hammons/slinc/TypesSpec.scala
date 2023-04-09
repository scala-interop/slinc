package fr.hammons.slinc

import types.*

//todo: Should test this via calls to test c method bindings
trait TypesSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.{*, given}
  test("can create maximally sized CLongs") {
    val size = DescriptorOf[CLong].size
    val x =
      if size == 4.toBytes then Some(CLong(Int.MaxValue))
      else if size == 8.toBytes then CLong.maybe(Long.MaxValue)
      else None

    assert(x.isDefined)
  }

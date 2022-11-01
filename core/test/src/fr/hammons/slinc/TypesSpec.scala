package fr.hammons.slinc

//todo: Should test this via calls to test c method bindings
trait TypesSpec(val slinc: Slinc) extends munit.FunSuite:
  import slinc.{*, given}
  test("can create maximally sized CLongs") {
    val size = summon[LayoutOf[CLong]].layout.size
    val x =
      if size == 4.toBytes then Some(Int.MaxValue.as[CLong])
      else if size == 8.toBytes then Long.MaxValue.maybeAs[CLong]
      else None

    assert(x.isDefined)
  }

package fr.hammons.sffi

case class X(a: Int, y: Y, b: Int)
case class Y(a: Int, b: Int)

trait AssignSpec(val f: FFI3) extends munit.FunSuite:
  import f.{*, given}

  given Struct[X] = Struct.derived[X]
  given Struct[Y] = Struct.derived[Y]
  
  val data = X(2,Y(2,3),3)

  test("can read and write native data") {
    val mem = Allocator.globalAllocator().allocate(summon[LayoutOf[X]].layout)
    mem.write[X](data, 0.toBytes)
    assertEquals(mem.read[X](0.toBytes), data)
  }

  test("jitted methods read and write native data") {
    f.forceJit()
    val mem = Allocator.globalAllocator().allocate(summon[LayoutOf[X]].layout)
    mem.write[X](data, 0.toBytes)
    assertEquals(mem.read[X](0.toBytes), data)
  }

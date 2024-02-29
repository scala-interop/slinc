import fr.hammons.slinc.runtime.given
import fr.hammons.slinc.types.*
import fr.hammons.slinc.*

import fr.hammons.slinc.annotations.NeedsResource

@NeedsResource("libadd0")
trait libadd derives FSet:
  def add(a: CInt, b: CInt): CInt
  def add_by_callback(a: CInt, f: Ptr[?]): CInt
  def add_str(a: Ptr[Byte], f: Ptr[Byte]): Ptr[Byte]

@main def run =
  val libadd = FSet.instance[libadd]
  assert(libadd.add(21, 21) == 42)

  val callback = Scope.global:
    Ptr.upcall(() => 21)
  assert(libadd.add_by_callback(21, callback) == 42)

  val (hello, world) = ("Hello", "World")
  val (a, b) = Scope.global:
    (Ptr.copy(hello), Ptr.copy(world))

  val c = libadd.add_str(a, b)
  val result =
    Ptr.copyIntoString(c)(hello.getBytes().length + world.getBytes().length + 1)
  assert(result == hello + world)

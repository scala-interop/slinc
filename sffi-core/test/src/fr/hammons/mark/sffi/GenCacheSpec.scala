package fr.hammons.sffi
import scala.annotation.retains

class X {
  val cache = GenCache()
}
val c = new X()
object Other:
  val cache = GenCache()

class GenCacheSpec extends munit.FunSuite:
  private given GenCache = GenCache()

  test("cache returns items appropriately") {
    assert(summon[GenCache].getOrAdd(4, new Object()).eq(summon[GenCache].getOrAdd(4, new Object())))
  }

  test("GenCache macro gives unique names to types and caches") {
    GenCache.cacheForType[Int, Int](3)
    GenCache.cacheForType[Float, Int](4)

    assertEquals(summon[GenCache].getOrAdd(0, 5), 3)
    assertEquals(summon[GenCache].getOrAdd(1,9), 4)
  }

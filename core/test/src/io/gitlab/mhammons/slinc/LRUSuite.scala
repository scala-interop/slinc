package io.gitlab.mhammons.slinc

import io.gitlab.mhammons.slinc.components.LRU

class LRUSuite extends munit.FunSuite:
   test("LRU cache caches appropriately") {
      val lru = new LRU(3)

      val o1 = lru.get("la", Object())
      val o2 = lru.get("ba", Object())
      val o3 = lru.get("ta", Object())

      assertNotEquals(o1, o2)
      assertNotEquals(o2, o3)

      assertEquals(lru.get("la", Object()), o1)
   }

   test("LRU cache evicts appropriately") {
      val lru = new LRU(3)

      val o1 = lru.get("la", Object())
      val o2 = lru.get("ba", Object())
      val o3 = lru.get("ta", Object())

      lru.get("la", Object())
      val o4 = lru.get("sa", Object())
      val o5 = lru.get("ba", Object())

      assertEquals(lru.get("la", Object()), o1)
      assertEquals(lru.get("sa", Object()), o4)
      assertEquals(lru.get("ba", Object()), o5)

      assertNotEquals(lru.get("ba", Object()), o2)
      assertNotEquals(lru.get("ta", Object()), o2)
   }

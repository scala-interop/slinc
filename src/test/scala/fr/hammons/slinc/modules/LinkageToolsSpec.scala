package fr.hammons.slinc.modules

import java.nio.file.Files
import java.io.FileInputStream

class LinkageToolsSpec extends munit.FunSuite:
  test("send resource to cache"):
    val testC = "test.c"
    val resultLocation =
      LinkageTools.sendResourceToCache(testC)

    assert(Files.exists(resultLocation.cachePath))
    Files.delete(resultLocation.cachePath)

    LinkageTools.sendResourceToCache(testC)
    assert(Files.exists(resultLocation.cachePath))

    Files.writeString(resultLocation.cachePath, "lala")

    assertEquals(Files.readString(resultLocation.cachePath), "lala")

    val cacheResult = LinkageTools.sendResourceToCache(testC)

    assertNotEquals(Files.readString(resultLocation.cachePath), "lala")
    assert(cacheResult.updated)

  test("hashing works"):
    val hash1 =
      LinkageTools.hash(getClass().getResourceAsStream(s"/native/test.c").nn)

    val hash2 = LinkageTools.hash(
      FileInputStream(
        LinkageTools
          .sendResourceToCache("test.c")
          .cachePath
          .toString()
      )
    )

    assertEquals(hash1, hash2)

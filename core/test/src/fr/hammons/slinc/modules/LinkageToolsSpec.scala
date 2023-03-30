package fr.hammons.slinc.modules

import java.nio.file.Files
import java.nio.file.Paths
import java.io.FileInputStream

class LinkageToolsSpec extends munit.FunSuite:
  test("send resource to cache"):
      val resultLocation = LinkageTools.sendResourceToCache("test.c")
      val resultPath = Paths.get(resultLocation)

      assert(Files.exists(resultPath))
      Files.delete(resultPath)

      LinkageTools.sendResourceToCache("test.c")
      assert(Files.exists(resultPath))

      Files.writeString(resultPath, "lala")

      assertEquals(Files.readString(resultPath), "lala")

      LinkageTools.sendResourceToCache("test.c")

      assertNotEquals(Files.readString(resultPath), "lala")

  test("hashing works"):
      val hash1 =
        LinkageTools.hash(getClass().getResourceAsStream(s"/native/test.c").nn)

      val hash2 = LinkageTools.hash(
        FileInputStream(LinkageTools.sendResourceToCache("test.c"))
      )

      assertEquals(hash1, hash2)

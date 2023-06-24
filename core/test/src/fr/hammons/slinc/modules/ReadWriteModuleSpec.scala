package fr.hammons.slinc.modules

import munit.FunSuite
import fr.hammons.slinc.modules.ReadWriteModule

class ReadWriteModuleSpec extends FunSuite {
    val descriptor = // create a TypeDescriptor
    val reader = ReadWriteModule.unionReader(descriptor)
    // assert that the reader behaves as expected
  }
    val descriptor = // create a TypeDescriptor
    val writer = ReadWriteModule.unionWriter(descriptor)
    // assert that the writer behaves as expected
  }
    val mem = // create a Mem
    val bytes = // create Bytes
    val descriptor = // create a TypeDescriptor
    ReadWriteModule.write(mem, bytes, descriptor)
    // assert that the memory was written to as expected
  }
    test("writeArray writes to memory as expected") {
    // create necessary inputs
    // call writeArray
    // assert that the memory was written to as expected
    }

    test("read reads from memory as expected") {
    // create necessary inputs
    // call read
    // assert that the output is as expected
    }

    test("readArray reads from memory as expected") {
    // create necessary inputs
    // call readArray
    // assert that the output is as expected
    }

    test("readFn reads from memory as expected") {
    // create necessary inputs
    // call readFn
    // assert that the output is as expected
    }
}
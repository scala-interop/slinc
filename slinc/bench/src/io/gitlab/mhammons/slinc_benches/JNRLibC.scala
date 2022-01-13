package io.gitlab.mhammons.slinc_benches

import jnr.ffi.{Struct, Runtime, Struct$Signed32}
trait JNRLibC:
   def getpid(): Long
   def strlen(string: String): Int
   def div(quot: Int, rem: Int): jnr_div_t

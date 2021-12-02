package io.gitlab.mhammons.slinc_benches

trait JNRLibC:
   def getpid(): Long
   def strlen(string: String): Int

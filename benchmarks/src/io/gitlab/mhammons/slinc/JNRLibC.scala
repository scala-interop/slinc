package io.gitlab.mhammons.slinc

trait JNRLibC:
   def getpid(): Long
   def strlen(string: String): Int

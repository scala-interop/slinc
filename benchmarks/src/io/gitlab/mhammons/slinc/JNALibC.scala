package io.gitlab.mhammons.slinc

import com.sun.jna.Library
import com.sun.jna.Native

trait JNALibC extends Library:

   def strlen(string: String): Int
   def getpid(): Long
   def div(a: Int, b: Int): jna_div_t

object JNALibC:
   val INSTANCE = Native.load("c", classOf[JNALibC])

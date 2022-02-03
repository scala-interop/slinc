package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*
import scala.concurrent.duration.FiniteDuration

object StdLib derives CLibrary:
   case class DivT(quot: Int, rem: Int) derives Struct
   case class LdivT(quot: Long, rem: Long) derives Struct
   def abort() = accessNative[Unit]
   def abs(x: Int) = accessNative[Int]
   def atof(str: Ptr[Byte]) = accessNative[Double]
   def atoi(str: Ptr[Byte]) = accessNative[Int]
   def atol(str: Ptr[Byte]) = accessNative[Long]
   def bsearch(
       key: Ptr[Any],
       base: Ptr[Any],
       nitems: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = accessNative[Ptr[Any]]
   def calloc(nitems: SizeT, size: SizeT) = accessNative[Ptr[Any]]
   def div(numer: Int, denom: Int) = accessNative[DivT]
   def exit(status: Int) = accessNative[Unit]
   def free(ptr: Ptr[Any]) = accessNative[Unit]
   def getenv(name: Ptr[Byte]) = accessNative[String]
   def labs(x: Long) = accessNative[Long]
   def ldiv(numer: Long, denom: Long) = accessNative[LdivT]
   def malloc(size: SizeT) = accessNative[Ptr[Any]]
   def mblen(str: Ptr[Byte], n: SizeT) = accessNative[Int]
   def qsort(
       base: Ptr[Any],
       nitems: SizeT,
       size: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = accessNative[Unit]
   def rand() = accessNative[Int]
   def realloc(ptr: Ptr[Any], size: SizeT) = accessNative[Ptr[Any]]
   def srand(seed: UInt) = accessNative[Unit]
   def strtod(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = accessNative[Double]
   def strtol(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = accessNative[Long]
   def strtoul(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = accessNative[ULong]
   def system(string: Ptr[Byte]) = accessNative[Int]
//todo: mbstowcs, mbtowc, wcstombs, wctomb

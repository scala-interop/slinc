package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*
import scala.concurrent.duration.FiniteDuration

object StdLib derives ILibrary:
   case class DivT(quot: Int, rem: Int) derives Struct
   case class LdivT(quot: Long, rem: Long) derives Struct
   def abort() = fromNative[Unit]
   def abs(x: Int) = fromNative[Int]
   // def atexit(fn: Ptr[() => Unit]) = bind2[Int]
   def atof(str: Ptr[Byte]) = fromNative[Double]
   def atoi(str: Ptr[Byte]) = fromNative[Int]
   def atol(str: Ptr[Byte]) = fromNative[Long]
   def bsearch(
       key: Ptr[Any],
       base: Ptr[Any],
       nitems: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = fromNative[Ptr[Any]]
   def calloc(nitems: SizeT, size: SizeT) = fromNative[Ptr[Any]]
   def div(numer: Int, denom: Int) = fromNative[DivT]
   def exit(status: Int) = fromNative[Unit]
   def free(ptr: Ptr[Any]) = fromNative[Unit]
   def getenv(name: Ptr[Byte]) = fromNative[String]
   def labs(x: Long) = fromNative[Long]
   def ldiv(numer: Long, denom: Long) = fromNative[LdivT]
   def malloc(size: SizeT) = fromNative[Ptr[Any]]
   def mblen(str: Ptr[Byte], n: SizeT) = fromNative[Int]
   def qsort(
       base: Ptr[Any],
       nitems: SizeT,
       size: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = fromNative[Unit]
   def rand() = fromNative[Int]
   def realloc(ptr: Ptr[Any], size: SizeT) = fromNative[Ptr[Any]]
   def srand(seed: UInt) = fromNative[Unit]
   def strtod(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = fromNative[Double]
   def strtol(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = fromNative[Long]
   def strtoul(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = fromNative[ULong]
   def system(string: Ptr[Byte]) = fromNative[Int]
//todo: mbstowcs, mbtowc, wcstombs, wctomb

package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object StdLib:
   case class DivT(quot: Int, rem: Int) derives Struct
   case class LdivT(quot: Long, rem: Long) derives Struct
   type div_t = DivT
   def atof(str: Ptr[Byte]) = bind[Double]
   def atoi(str: Ptr[Byte]) = bind[Int]
   def atol(str: Ptr[Byte]) = bind[Long]
   def strtod(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = bind[Double]
   def strtol(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = bind[Long]
   def strtoul(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = bind[ULong]
   def calloc(nitems: SizeT, size: SizeT) = bind[Ptr[Any]]
   def free(ptr: Ptr[Any]) = bind[Unit]
   def malloc(size: SizeT) = bind[Ptr[Any]]
   def realloc(ptr: Ptr[Any], size: SizeT) = bind[Ptr[Any]]
   def abort() = bind[Unit]
   def atexit(fn: Ptr[() => Unit]) = bind[Int]
   def exit(status: Int) = bind[Unit]
   def getenv(name: Ptr[Byte]) = bind[String]
   def system(string: Ptr[Byte]) = bind[Int]
   def bsearch(
       key: Ptr[Any],
       base: Ptr[Any],
       nitems: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = bind[Ptr[Any]]
   def qsort(
       base: Ptr[Any],
       nitems: SizeT,
       size: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = bind[Unit]
   def abs(x: Int) = bind[Int]
   def div(numer: Int, denom: Int) = bind[DivT]
   def labs(x: Long) = bind[Long]
   def ldiv(numer: Long, denom: Long) = bind[LdivT]
   def rand() = bind[Int]
   def srand(seed: UInt) = bind[Unit]
   def mblen(str: Ptr[Byte], n: SizeT) = bind[Int]
//todo: mbstowcs, mbtowc, wcstombs, wctomb

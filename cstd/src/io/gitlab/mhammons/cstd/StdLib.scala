package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object StdLib derives ILibrary:
   case class DivT(quot: Int, rem: Int) derives Struct
   case class LdivT(quot: Long, rem: Long) derives Struct
   def abort() = bind2[Unit]
   def abs(x: Int) = bind2[Int]
   // def atexit(fn: Ptr[() => Unit]) = bind2[Int]
   def atof(str: Ptr[Byte]) = bind2[Double]
   def atoi(str: Ptr[Byte]) = bind2[Int]
   def atol(str: Ptr[Byte]) = bind2[Long]
   def bsearch(
       key: Ptr[Any],
       base: Ptr[Any],
       nitems: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = bind2[Ptr[Any]]
   def calloc(nitems: SizeT, size: SizeT) = bind2[Ptr[Any]]
   def div(numer: Int, denom: Int) = bind2[DivT]
   def exit(status: Int) = bind2[Unit]
   def free(ptr: Ptr[Any]) = bind2[Unit]
   def getenv(name: Ptr[Byte]) = bind2[String]
   def labs(x: Long) = bind2[Long]
   def ldiv(numer: Long, denom: Long) = bind2[LdivT]
   def malloc(size: SizeT) = bind2[Ptr[Any]]
   def mblen(str: Ptr[Byte], n: SizeT) = bind2[Int]
   def qsort(
       base: Ptr[Any],
       nitems: SizeT,
       size: SizeT,
       compar: Ptr[(Ptr[Any], Ptr[Any]) => Int]
   ) = bind2[Unit]
   def rand() = bind2[Int]
   def realloc(ptr: Ptr[Any], size: SizeT) = bind2[Ptr[Any]]
   def srand(seed: UInt) = bind2[Unit]
   def strtod(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = bind2[Double]
   def strtol(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = bind2[Long]
   def strtoul(str: Ptr[Byte], endptr: Ptr[Ptr[Byte]]) = bind2[ULong]
   def system(string: Ptr[Byte]) = bind2[Int]
//todo: mbstowcs, mbtowc, wcstombs, wctomb

package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object Time derives CLibrary:
   case class Tm(
       tmSec: Int,
       tmMin: Int,
       tmHour: Int,
       tmMday: Int,
       tmMon: Int,
       tmYear: Int,
       tmWday: Int,
       tmYday: Int,
       tmIsdst: Int
   ) derives Struct

   def asctime(tm: Ptr[Tm]) = accessNative[Ptr[Byte]]
   def clock() = accessNative[ClockT]
   def ctime(timer: Ptr[TimeT]) = accessNative[Ptr[Byte]]
   def difftime(time1: TimeT, time2: TimeT) = accessNative[Double]
   def gmtime(time: Ptr[TimeT]) = accessNative[Ptr[Tm]]
   def localtime(timer: Ptr[TimeT]) = accessNative[Ptr[Tm]]
   def mktime(timeptr: Ptr[Tm]) = accessNative[TimeT]
   def strftime(
       str: Ptr[Byte],
       maxsize: SizeT,
       format: Ptr[Byte],
       timeptr: Ptr[Tm]
   ) = accessNative[SizeT]
   def time(timer: Ptr[TimeT]) = accessNative[TimeT]

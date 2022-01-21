package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object Time:
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

   def asctime(tm: Ptr[Tm]) = bind[Ptr[Byte]]
   def clock() = bind[ClockT]
   def ctime(timer: Ptr[TimeT]) = bind[Ptr[Byte]]
   def difftime(time1: TimeT, time2: TimeT) = bind[Double]
   def gmtime(time: Ptr[TimeT]) = bind[Ptr[Tm]]
   def localtime(timer: Ptr[TimeT]) = bind[Ptr[Tm]]
   def mktime(timeptr: Ptr[Tm]) = bind[TimeT]
   def strftime(
       str: Ptr[Byte],
       maxsize: SizeT,
       format: Ptr[Byte],
       timeptr: Ptr[Tm]
   ) = bind[SizeT]
   def time(timer: Ptr[TimeT]) = bind[TimeT]

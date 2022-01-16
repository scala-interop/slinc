package io.gitlab.mhammons.slinc.components.platform

import io.gitlab.mhammons.slinc.components.{NativeInfo, infoOf}
import io.gitlab.mhammons.slinc.components.Immigrator
import io.gitlab.mhammons.slinc.components.Exporter
import io.gitlab.mhammons.slinc.platform.time_t_proto
import io.gitlab.mhammons.slinc.platform.time_t_impl

trait PlatformTypes extends time_t_proto

object PlatformLinuxX86 extends PlatformTypes with time_t_impl[Long]
val myp =
   if true then PlatformLinuxX86
   else
      new PlatformTypes:
         type time_t = Int
         given ni_time_t: NativeInfo[time_t] = infoOf[Int]
         extension (t: time_t) def %(rhs: time_t): time_t = t % rhs

package io.gitlab.mhammons.slinc.platform

import io.gitlab.mhammons.slinc.components.NativeInfo
import io.gitlab.mhammons.slinc.components.Immigrator
import io.gitlab.mhammons.slinc.components.Exporter

trait time_t_impl[U](using
    val time_t_integral: Integral[U],
    val time_t_NativeInfo: NativeInfo[U],
    val time_t_Immigrator: Immigrator[U],
    val time_t_Exporter: Exporter[U]
) extends time_t_proto:
   type time_t = U

trait time_t_proto:
   type time_t
   val time_t_integral: Integral[time_t]
   given time_t_NativeInfo: NativeInfo[time_t]
   given time_t_Immigrator: Immigrator[time_t]
   given time_t_Exporter: Exporter[time_t]

   export time_t_integral.*

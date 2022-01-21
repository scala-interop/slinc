package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object StdIO:
   def sprintf(str: Ptr[Byte], format: Ptr[Byte]) =
      variadicBind[Int](str, format)

   def sscanf(str: Ptr[Byte], format: Ptr[Byte]) =
      variadicBind[Int](str, format)

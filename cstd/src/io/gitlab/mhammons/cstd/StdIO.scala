package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object StdIO derives CLibrary:
   def sprintf(str: Ptr[Byte], format: Ptr[Byte]) =
      accessNativeVariadic[Int](str, format)

   def sscanf(str: Ptr[Byte], format: Ptr[Byte]) =
      accessNativeVariadic[Int](str, format)

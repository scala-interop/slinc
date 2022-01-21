package io.gitlab.mhammons.cstd

import io.gitlab.mhammons.slinc.*

object CLocale:
   // todo: these are my locally defined values. Not portable!
   val LCCtype = 0
   val LCNumeric = 1
   val LCTime = 2
   val LCCollate = 3
   val LCMonetary = 4
   val LCMessages = 5
   val LCAll = 6

   def setlocale(category: Int, locale: Ptr[Byte]) = bind[Ptr[Byte]]

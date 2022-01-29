package io.gitlab.mhammons.slinc.components

import java.lang.invoke.MethodHandle

trait StandardCall(mh: MethodHandle)
object StandardCalls:
   class StandardCall2[A, B, AA](mh: MethodHandle)(using
       Emigrator[A],
       NativeInfo[A],
       Emigrator[B],
       NativeInfo[B],
       Immigrator[AA],
       NativeInfo[AA]
   ) extends StandardCall(mh):
      def apply(a: A, b: B): Native[AA] = ???

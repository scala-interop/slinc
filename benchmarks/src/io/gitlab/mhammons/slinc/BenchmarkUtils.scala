package io.gitlab.mhammons.slinc

def repeat[A](fn: => A, times: Int) =
   var count = times
   while count > 0 do
      fn
      count -= 1

inline def repeatInl[A](fn: => A, times: Int) =
   var count = times
   while count > 0 do
      fn
      count -= 1

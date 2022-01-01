package io.gitlab.mhammons.polymorphics

import java.lang.invoke.MethodHandle

object MethodHandleHandler {
   def exCallI(mh: MethodHandle, a: Int) = mh.invokeExact(a)
   def excallL(mh: MethodHandle): Long = mh.invokeExact()
   def call0(mh: MethodHandle) = mh.invoke()
   def call1(mh: MethodHandle, a: Any) = mh.invoke(a)
   def call2(mh: MethodHandle, a: Any, b: Any) = mh.invoke(a, b)
   def call3(mh: MethodHandle, a: Any, b: Any, c: Any) = mh.invoke(a, b, c)
   def call4(mh: MethodHandle, a: Any, b: Any, c: Any, d: Any) =
      mh.invoke(a, b, c, d)

   def call5(mh: MethodHandle, a: Any, b: Any, c: Any, d: Any, e: Any) =
      mh.invoke(a, b, c, d, e)
   def call6(mh: MethodHandle, a: Any, b: Any, c: Any, d: Any, e: Any, f: Any) =
      mh.invoke(a, b, c, d, e, f)
   def call7(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g)
   def call8(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h)
   def call9(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any
   ) =
      mh.invoke(a, b, c, e, f, g, h, i)

   def call10(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j)

   def call11(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k)

   def call12(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l)

   def call13(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m)

   def call14(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n)

   def call15(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)

   def call16(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)

   def call17(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any,
       q: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)

   def call18(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any,
       q: Any,
       r: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)

   def call19(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any,
       q: Any,
       r: Any,
       s: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)

   def call20(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any,
       q: Any,
       r: Any,
       s: Any,
       t: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)

   def call21(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any,
       q: Any,
       r: Any,
       s: Any,
       t: Any,
       u: Any
   ) =
      mh.invoke(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u)

   def call22(
       mh: MethodHandle,
       a: Any,
       b: Any,
       c: Any,
       d: Any,
       e: Any,
       f: Any,
       g: Any,
       h: Any,
       i: Any,
       j: Any,
       k: Any,
       l: Any,
       m: Any,
       n: Any,
       o: Any,
       p: Any,
       q: Any,
       r: Any,
       s: Any,
       t: Any,
       u: Any,
       v: Any
   ): Any = mh.invoke(
     a,
     b,
     c,
     d,
     e,
     f,
     g,
     h,
     i,
     j,
     k,
     l,
     m,
     n,
     o,
     p,
     q,
     r,
     s,
     t,
     u,
     v
   )
}

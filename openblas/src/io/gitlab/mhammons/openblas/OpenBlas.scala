package io.gitlab.mhammons.openblas

import io.gitlab.mhammons.slinc.*

object OpenBlas extends SystemLibrary("cblas"), WithPrefix["cblas"]
    derives CLibrary:
   type CblasIndex = Long
   def ddot(
       n: CblasIndex,
       dx: Ptr[Double],
       incx: Int,
       dy: Ptr[Double],
       incy: Int
   ): Double = accessNative[Double]

   case class ComplexFloat(real: Float, imaginary: Float) derives Struct

   def cdotuSub(
       n: CblasIndex,
       x: Ptr[Any],
       incX: CblasIndex,
       y: Ptr[Any],
       incY: CblasIndex,
       dotu: Ptr[Any]
   ) = accessNative[Unit]

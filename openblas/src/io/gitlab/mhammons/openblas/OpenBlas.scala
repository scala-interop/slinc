package io.gitlab.mhammons.openblas

import io.gitlab.mhammons.slinc.*

object OpenBlas extends SystemLibrary("cblas") derives CLibrary:
   type CblasIndex = Long
   def cblas_ddot(
       n: CblasIndex,
       dx: Ptr[Double],
       incx: Int,
       dy: Ptr[Double],
       incy: Int
   ): Double = accessNative[Double]

   case class ComplexFloat(real: Float, imaginary: Float) derives Struct

   def cblas_cdotu_sub(
       n: CblasIndex,
       x: Ptr[Any],
       incX: CblasIndex,
       y: Ptr[Any],
       incY: CblasIndex,
       dotu: Ptr[Any]
   ) = accessNative[Unit]

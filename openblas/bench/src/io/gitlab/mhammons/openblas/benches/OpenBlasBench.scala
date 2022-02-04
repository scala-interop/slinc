package io.gitlab.mhammons.openblas.benches

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import scala.util.Random
import org.jblas.NativeBlas
import io.gitlab.mhammons.slinc.*
import io.gitlab.mhammons.slinc.components.Encoder
import io.gitlab.mhammons.openblas.OpenBlas

@State(Scope.Thread)
@Fork(
  jvmArgsAppend = Array(
    "--add-modules=jdk.incubator.foreign",
    "--enable-native-access=ALL-UNNAMED"
  )
)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class OpenBlasBench:
   val size = 10_000
   val arr1 = Array.fill(size)(Random.nextDouble * Random.nextInt)
   val arr2 = Array.fill(size)(Random.nextDouble * Random.nextInt)

   given enc: Encoder[Array[Double]] = summon[Encoder[Array[Double]]]

   @Benchmark
   def jblasDDot =
      NativeBlas.ddot(size, arr1, 0, 1, arr2, 0, 1)

   @Benchmark
   def slincblasDDot =
      scope {
         OpenBlas.ddot(size, arr1.encode, 1, arr2.encode, 1)
      }

   val complexArrayBase1 =
      Array.fill(size)(Array.fill(2)(Random.nextFloat * Random.nextInt))

   val complexArrayBase2 =
      Array.fill(size)(Array.fill(2)(Random.nextFloat * Random.nextInt))

   val complexStructArray1 =
      complexArrayBase1.map(arr => OpenBlas.ComplexFloat(arr(0), arr(1)))

   val complexStructArray2 =
      complexArrayBase2.map(arr => OpenBlas.ComplexFloat(arr(0), arr(1)))

   val complexArray1 = complexArrayBase1.flatten
   val complexArray2 = complexArrayBase2.flatten

   @Benchmark
   def jblasCDotU =
      NativeBlas.cdotu(size, complexArray1, 0, 1, complexArray2, 0, 1)

   @Benchmark
   def openblasCDotUStructStyle =
      scope {
         val complexPtr = allocate[OpenBlas.ComplexFloat](1)
         OpenBlas.cdotuSub(
           size,
           complexStructArray1.encode.castTo[Any],
           1,
           complexStructArray2.encode.castTo[Any],
           1,
           complexPtr.castTo[Any]
         )
         !complexPtr
      }

   @Benchmark
   def openblasCDotUArrayStyle =
      scope {
         val complexPtr = allocate[Float](2)
         OpenBlas.cdotuSub(
           size,
           complexArray1.encode.castTo[Any],
           1,
           complexArray2.encode.castTo[Any],
           1,
           complexPtr.castTo[Any]
         )
         !complexPtr
      }

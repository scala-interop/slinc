package fr.hammons.slinc

import fr.hammons.slinc.types.CLong
import org.openjdk.jmh.annotations.{Scope as _, *}

case class A(a: Int, b: B, c: Int) derives Struct
case class B(a: Int, b: Int) derives Struct

@Warmup(iterations = 5)
@Measurement(iterations = 5)
trait TransferBenchmarkShape(val s: Slinc):
  import s.{given, *}

  case class C(a: Int, b: D, c: Int) derives Struct
  case class D(a: CLong, b: Int) derives Struct
  case class E(a: Int, b: Int) derives Struct
  case class F(a: Int, e: E, c: Int) derives Struct

  val aPtr = Scope.global {
    Ptr.blank[A]
  }

  val a = A(1, B(2, 3), 4)

  val cPtr = Scope.global {
    Ptr.blank[C]
  }

  val c = C(1, D(CLong(2), 3), 4)

  @Benchmark
  def topLevelRead =
    !aPtr

  @Benchmark
  def topLevelWrite =
    !aPtr = a

  @Benchmark
  def innerRead =
    !cPtr

  @Benchmark
  def innerWrite =
    !cPtr = c

  @Benchmark
  def allocateFnPointer =
    Scope.confined(
      Ptr.upcall((a: Ptr[Int]) => !a + 1)
    )

  @Benchmark
  def allocatePrimitivePointer =
    Scope.confined(
      Ptr.copy(3)
    )

  @Benchmark
  def allocateAliasPointer =
    Scope.confined(
      Ptr.copy(CLong(3))
    )

  @Benchmark
  def allocateComplexWAliasInnerStructPointer =
    Scope.confined(
      Ptr.copy(C(1, D(CLong(2), 3), 4))
    )

  @Benchmark
  def allocateSimpleWAliasInnerStructPointer =
    Scope.confined(
      Ptr.copy(D(CLong(2), 3))
    )

  @Benchmark
  def allocatePtrFromArray =
    Scope.confined(
      Ptr.copy(Array(1, 2, 3))
    )

  @Benchmark
  def allocatePtrFromCLongArray =
    Scope.confined(
      Ptr.copy(Array(CLong(1), CLong(2), CLong(3)))
    )

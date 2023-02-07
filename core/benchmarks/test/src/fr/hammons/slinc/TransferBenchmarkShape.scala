package fr.hammons.slinc

import org.openjdk.jmh.annotations.*,
  Mode.{SampleTime, SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit
import fr.hammons.slinc.Scope
import scala.annotation.experimental

case class A(a: Int, b: B, c: Int)
case class B(a: Int, b: Int)

trait TransferBenchmarkShape(val s: Slinc):
  import s.{given, *}

  case class C(a: Int, b: D, c: Int)
  case class D(a: Int, b: Int)
  given Struct[A] = Struct.derived
  given Struct[B] = Struct.derived
  given Struct[C] = Struct.derived
  given Struct[D] = Struct.derived

  val aPtr = Scope.global {
    Ptr.blank[A]
  }

  val a = A(1, B(2, 3), 4)

  val cPtr = Scope.global {
    Ptr.blank[C]
  }

  val c = C(1, D(2, 3), 4)

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
  def allocateIntPointer =
    Scope.confined(
      Ptr.copy(3)
    )

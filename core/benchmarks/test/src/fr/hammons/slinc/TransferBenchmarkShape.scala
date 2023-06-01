package fr.hammons.slinc

import fr.hammons.slinc.types.CLong
import org.openjdk.jmh.annotations.{Scope as _, *}
import org.openjdk.jmh.infra.Blackhole
import fr.hammons.slinc.descriptors.WriterContext
import scala.util.Random

case class A(a: Int, b: B, c: Int) derives Struct
case class B(a: Int, b: Int) derives Struct
case class G(a: Int, b: Float, c: CLong) derives Struct
case class I(a: Int, b: Float, c: CLong) derives Struct

//@Warmup(iterations = 5)
//@Measurement(iterations = 5)
trait TransferBenchmarkShape(val s: Slinc):
  import s.{given, *}

  given WriterContext = WriterContext(dm, rwm)

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

  val g = G(1, 2f, CLong(3))

  val gPtr = Scope.global {
    Ptr.blank[G]
  }

  val i = I(1, 2f, CLong(3))
  val iPtr = Scope.global:
    Ptr.blank[I]

  val optimizedIWriter =
    summon[DescriptorOf[I]].writer.forceOptimize

  @Benchmark
  def topLevelRead =
    !aPtr

  @Benchmark
  def topLevelWrite =
    !aPtr = a

  @Benchmark
  @Fork(
    jvmArgsAppend = Array(
      "-Dslinc.jitc.mode=standard"
    )
  )
  def topLevelWriteGJitted(blackhole: Blackhole) = blackhole.consume:
    !gPtr = g

  @Benchmark
  @Fork(
    jvmArgsAppend = Array(
      "-Dslinc.jitc.mode=disabled"
    )
  )
  def topLevelWriteGNoJit(blackhole: Blackhole) = blackhole.consume:
    !gPtr = g

  @Benchmark
  @Fork(
    jvmArgsAppend = Array(
      "-Dslinc.jitc.mode=immediate"
    )
  )
  def topLevelWriteGImmediateJIT(blackhole: Blackhole) = blackhole.consume:
    !gPtr = g

  @Benchmark
  def cachedWriteI(blackhole: Blackhole) = blackhole.consume:
    optimizedIWriter(iPtr.mem, Bytes(0), i)

  var x = Random.nextInt()
  var y = Random.nextInt()

  @Benchmark
  def addValues(blackhole: Blackhole) = blackhole.consume:
    x + y

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

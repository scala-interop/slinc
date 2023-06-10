package fr.hammons.slinc

import fr.hammons.slinc.types.CLong
import org.openjdk.jmh.annotations.{Scope as _, *}
import org.openjdk.jmh.infra.Blackhole
import fr.hammons.slinc.descriptors.WriterContext
import scala.util.Random
import fr.hammons.slinc.jitc.FnToJit
import fr.hammons.slinc.modules.MemWriter
import scala.compiletime.uninitialized

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

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  def offset = Bytes(0)

  val g = G(1, 2f, CLong(3))

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  def getG = g

  val gPtr = Scope.global {
    Ptr.blank[G]
  }

  val i = I(1, 2f, CLong(3))

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  def getI = i
  val iPtr = Scope.global:
    Ptr.blank[I]

  val optimizedIWriter =
    summon[DescriptorOf[I]].writer.forceOptimize

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  def getOptimizedIWriter = optimizedIWriter

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
  def jitted(blackhole: Blackhole) = blackhole.consume:
    !gPtr = getG

  @Benchmark
  @Fork(
    jvmArgsAppend = Array(
      "-Dslinc.jitc.mode=disabled"
    )
  )
  def compiletime(blackhole: Blackhole) = blackhole.consume:
    !gPtr = getG

  @Benchmark
  @Fork(
    jvmArgsAppend = Array(
      "-Dslinc.jitc.mode=immediate"
    )
  )
  def immediatecompilation(blackhole: Blackhole) = blackhole.consume:
    !gPtr = getG

  @Benchmark
  def nakedfunction(blackhole: Blackhole) = blackhole.consume:
    getOptimizedIWriter(iPtr.mem, iPtr.offset, getI)

  import scala.language.unsafeNulls
  val castGWriter: FnToJit[MemWriter[G], WriterContext] =
    summon[DescriptorOf[G]].writer match
      case a: FnToJit[MemWriter[G], WriterContext] => a
      case _                                       => null

  var x = Random.nextInt()
  var y = Random.nextInt()

  @Benchmark
  def fntojit(blackhole: Blackhole) = blackhole.consume(
    castGWriter.get(gPtr.mem, gPtr.offset, getG)
  )

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

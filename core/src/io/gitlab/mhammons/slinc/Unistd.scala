package io.gitlab.mhammons.slinc
import jdk.incubator.foreign.MemoryAddress
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.CLinker.{C_INT, C_LONG}
import jdk.incubator.foreign.MemoryAccess
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.ResourceScope
import scala.util.chaining.*
import javax.swing.GroupLayout
import io.gitlab.mhammons.polymorphics.*

object Unistd extends CLib:
  val getPid = downcall[Long]("getpid")
  val _exit = downcall[Long, Unit]("_exit")

object Stdlib extends io.gitlab.mhammons.slinc.CLib:
  val abort = downcall[Unit]("abort")
  val free = downcall[MemoryAddress, Unit]("free")

  type div_t = ("quot" --> Int) ~% ("rem" --> Int)
  val div = downcallS[Int, Int, div_t]("div")

object Time extends CLib:
  type tm = ("tm_sec" --> Int) ~%
    ("tm_min" --> Int) ~%
    ("tm_hour" --> Int) ~%
    ("tm_mday" --> Int) ~%
    ("tm_mon" --> Int) ~%
    ("tm_year" --> Int) ~%
    ("tm_wday" --> Int) ~%
    ("tm_yday" --> Int) ~%
    ("tm_isdst" --> Int)
  val time = downcall[MemoryAddress, Long]("time")
  val localTime = downcall[MemoryAddress, MemoryAddress]("localtime")

@main def fn =
  println(Unistd.getPid.map(_()))
  //Unistd._exit.foreach(_(1))
  val t = Time.time.map(_(MemoryAddress.NULL)).get.asInstanceOf[Long]
  val scope = ResourceScope.newConfinedScope
  implicit val segAlloc = SegmentAllocator.arenaAllocator(scope)
  val seg = MemorySegment.allocateNative(C_LONG, scope)
  MemoryAccess.setLong(seg, t)
  println(MemoryAccess.getLong(seg))

  val tmLayout = scala2MemoryLayout[Time.tm]

  val tmVH = VarHandleHandler(
    scala2MemoryLayout[Time.tm].varHandle(
      classOf[Int],
      MemoryLayout.PathElement.groupElement("tm_sec")
    )
  )

  println(tmVH)

  val tmPtr = Time.localTime
    .map(_(seg.address))
    .map(_.asSegment(tmLayout.byteSize, scope))

  val handle = tmPtr.map(_.pipe(tmVH.get)).foreach(println)

  val l = Stdlib.div.map(_(5, 2)).get

  val divTVH = VarHandleHandler(
    scala2MemoryLayout[Stdlib.div_t]
      .varHandle(classOf[Int], MemoryLayout.PathElement.groupElement("quot"))
  )

  println(divTVH.get(l))
  Unistd._exit.foreach(_(5))
  println("hello")

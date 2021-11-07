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
import cats.implicits.*
import cats.catsInstancesForId

type divor = StructBacking {
   val quot: int
   val rem: int
}

object Unistd extends CLib:
   val getPid = downcall[Long]("getpid")
   val getPid2 =
      NativeIO.function[() => Long]("getpid")

   val getPie = ()
   // NativeIO
   //    .function[Int => divor]("getpid")
   val _exit = downcall[Long, Unit]("_exit")

object Stdlib extends io.gitlab.mhammons.slinc.CLib:
   val abort = downcall[Unit]("abort")
   val free = downcall[MemoryAddress, Unit]("free")

object Time extends CLib:
   val time = downcall[MemoryAddress, Long]("time")
   val localTime = downcall[MemoryAddress, MemoryAddress]("localtime")
// val div = NativeIO.function[(Int, Int) => divor]("div")

// object String:
//    val strcmp = NativeIO.function[(String) => Int]("strlen")

trait div_t extends Struct[div_t]:
   import Fd.*
   val quot: int = Fd(null, VarHandleHandler(null))
   val rem: int = Fd(null, VarHandleHandler(null))
   val a: int = Fd(null, VarHandleHandler(null))

import Fd.*
case class div_u(quot: int, rem: int) derives Structish

//case class div_x(quot: Int, rem: Int) derives Structish

import Fd.int

class divo:
   val quot: Int = 5

class Stuk extends Selectable:
   def selectDynamic(str: String) = 5
   def applyDynamic(str: String) = 5

@main def fn =
   println(Unistd.getPid.map(_()))
   // Unistd._exit.foreach(_(1))
   val t = Time.time.map(_(MemoryAddress.NULL)).get.asInstanceOf[Long]
   val scope = ResourceScope.newConfinedScope
   implicit val segAlloc = SegmentAllocator.arenaAllocator(scope)
   val seg = MemorySegment.allocateNative(C_LONG, scope)
   MemoryAccess.setLong(seg, t)
   println(MemoryAccess.getLong(seg))

   // val l = Stdlib.div.map(_(5, 2)).get

   val l = deriveLayout[divor]
   val allocatedMem = segAlloc.allocate(l)
   val s = structFromMemorySegment[divor](allocatedMem)
   println(s.quot.get)
   s.quot.set(5)
   println(s.quot.get)
   println(s.$mem == allocatedMem)

   val d = summon[Structish[div_u]].allocate(segAlloc)

   println(d.quot.get)
   d.quot.set(4)
   println(d.quot.get)

   println(summon[Structish[div_u]].layout)
   println(summon[Structish[div_u]].layout)

   // def allocie[A <: StructBacking] = NativeIO.allocate[A]

   val x = NativeIO.scope(
     for
        // a <- NativeIO.allocate[divor]
        // b <- NativeIO.allocate[divor]
        l <- NativeIO.layout[divor]
        pid <- Unistd.getPid2()

     // _ = println(a.quot.get)
     // _ = b.quot.set(3)
     // _ = println(a.quot.get)
     // _ = println(b.quot.get)
     yield println(pid)
   )

   x.foldMap(NativeIO.impureCompiler)

   Unistd._exit.foreach(_(5))
   println("hello")

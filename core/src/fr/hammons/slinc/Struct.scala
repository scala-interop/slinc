package fr.hammons.slinc

import scala.deriving.Mirror
import scala.compiletime.uninitialized
import java.util.concurrent.atomic.AtomicReference
import scala.reflect.ClassTag

class StructI(
    layoutI: LayoutI,
    transitionI: TransitionsI,
    jitManager: JitManager
):
  import layoutI.{given, *}
  import transitionI.given
  trait Struct[A <: Product]
      extends LayoutOfStruct[A],
        Send[A],
        Receive[A],
        InAllocatingTransitionNeeded[A],
        OutTransitionNeeded[A]

  object Struct:
    inline def derived[A <: Product](using
        m: Mirror.ProductOf[A],
        ct: ClassTag[A]
    ) = new Struct[A]:
      val layout: StructLayout =
        structLayout[A]

      private val offsetsArray = layout.offsets

      private var sender: Send[A] = uninitialized

      private var receiver: Receive[A] = uninitialized

      jitManager.jitc(
        Send.compileTime[A](offsetsArray),
        Send.staged[A](layout),
        sender = _
      )

      jitManager.jitc(
        Receive.compileTime[A](offsetsArray),
        Receive.staged[A](layout),
        receiver = _
      )

      final def to(mem: Mem, offset: Bytes, a: A): Unit =
        sender.to(mem, offset, a)

      final def from(mem: Mem, offset: Bytes): A =
        receiver.from(mem, offset).asInstanceOf[A]

      final def in(a: A)(using alloc: Allocator): Object =
        val mem = alloc.allocate(this.layout, 1)
        to(mem, Bytes(0), a)
        transitionI.structMemIn(mem)

      final def out(a: Object): A =
        val mem = transitionI.structMemOut(a)
        from(mem, Bytes(0))

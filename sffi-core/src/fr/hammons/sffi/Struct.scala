package fr.hammons.sffi

import scala.deriving.Mirror
import scala.compiletime.uninitialized
import java.util.concurrent.atomic.AtomicReference
import scala.reflect.ClassTag

class StructI(layoutI: LayoutI, jitManager: JitManager):
  import layoutI.{given, *}
  trait Struct[A <: Product] extends LayoutOf[A], Send[A], Receive[A]

  object Struct:
    inline def derived[A <: Product](using
        m: Mirror.ProductOf[A],
        ct: ClassTag[A]
    ) = new Struct[A]:
      val layout: StructLayout =
        structLayout[A]

      private val offsetsArray = IArray.from(layout.offsets)

      @volatile private var sender: Send[A] = uninitialized

      @volatile private var receiver: Receive[A] = uninitialized

      jitManager.jitc(
        (s: Send[A]) => sender = s,
        Send.compileTime[A](offsetsArray),
        Send.staged[A](layout)
      )

      jitManager.jitc(
        (r: Receive[A]) => receiver = r,
        Receive.compileTime[A](offsetsArray),
        Receive.staged[A](layout)
      )

      final def to(mem: Mem, offset: Bytes, a: A): Unit =
        sender.to(mem, offset, a)

      final def from(mem: Mem, offset: Bytes): A =
        receiver.from(mem, offset).asInstanceOf[A]

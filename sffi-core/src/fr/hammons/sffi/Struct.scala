package fr.hammons.sffi

import scala.deriving.Mirror
import java.util.concurrent.atomic.AtomicReference
import scala.reflect.ClassTag

class StructI(layoutI: LayoutI, jitManager: JitManager):
  import layoutI.{given, *}
  abstract class Struct[A <: Product] extends LayoutOf[A], Send[A], Receive[A]

  object Struct:
    inline def derived[A <: Product](using
        m: Mirror.ProductOf[A],
        ct: ClassTag[A]
    ) = new Struct[A]:
      val layout: StructLayout =
        structLayout[A]

      private val offsetsArray = IArray.from(layout.offsets)

      private val sender: AtomicReference[Send[Product]] =
        AtomicReference()

      private val receiver: AtomicReference[Receive[Product]] =
        AtomicReference()

      jitManager.jitc(
        sender,
        Send.compileTime[A](offsetsArray),
        Send.staged(layout)
      )

      jitManager.jitc2(
        receiver,
        Receive.compileTime[A](offsetsArray),
        Receive.staged(layout)
      )
      final def to(mem: Mem, offset: Bytes, a: A): Unit =
        import scala.language.unsafeNulls
        sender.get.to(mem, offset, a)

      final def from(mem: Mem, offset: Bytes): A =
        import scala.language.unsafeNulls
        receiver.get().from(mem, offset).asInstanceOf[A]

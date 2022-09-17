package fr.hammons.sffi

import scala.deriving.Mirror
import scala.compiletime.{
  erasedValue,
  summonInline,
  constValue,
  codeOf,
  summonFrom
}
import java.util.concurrent.atomic.AtomicReference
import scala.quoted.staging.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.Executors
import scala.reflect.ClassTag

class StructI(layoutI: LayoutI, jitManager: JitManager):
  import layoutI.{given, *}
  trait Struct[A <: Product]
      extends LayoutOf[A],
        Send[A],
        Receive[A]

  object Struct:
    inline def calcTupLayout[A <: Tuple]: List[DataLayout] =
      inline erasedValue[A] match
        case _: ((name, value) *: t) =>
          summonInline[LayoutOf[value]].layout
            .withName(constValue[name].toString) :: calcTupLayout[t]
        case _: EmptyTuple => Nil

    inline def derived[A <: Product](
        using
        m: Mirror.ProductOf[A],
        ct: ClassTag[A]
    ) = new Struct[A]:
      val layout: StructLayout =
        structLayout[A](
          calcTupLayout[Tuple.Zip[m.MirroredElemLabels, m.MirroredElemTypes]]*
        )

      private val sender: AtomicReference[Send[Object]] =
        AtomicReference()

      private val receiver: AtomicReference[Receive[Product]] =
        AtomicReference()

      jitManager.jitc(
        sender,
        StructI.calcSender[A](layout.offsets),
        Send.sendStaged(layout)
      )

      jitManager.jitc2(
        receiver,
        StructI.calcReceiver[A](layout.offsets),
        Receive.receiveStaged(layout)
      )
      def to(mem: Mem, offset: Bytes, a: A): Unit =
        import scala.language.unsafeNulls
        sender.get.to(mem, offset, a.asInstanceOf[Object])

      def from(mem: Mem, offset: Bytes): A =
        receiver.get().nn.from(mem, offset).asInstanceOf[A]

object StructI:
  inline def calcSender[A <: Product](
      offsets: Vector[Bytes]
  )(using m: Mirror.ProductOf[A]): Send[Object] =
    (rawMem: Mem, offset: Bytes, a: Object) =>
      calcTupSender[m.MirroredElemTypes](
        Tuple.fromProduct(a.asInstanceOf[A]).toArray,
        rawMem,
        offset,
        offsets,
        0
      )
  inline def calcTupSender[A <: Tuple](
      a: Array[Object],
      rawMem: Mem,
      offset: Bytes,
      offsets: Vector[Bytes],
      position: Int
  ): Unit =
    inline erasedValue[A] match
      case _: (h *: t) =>
        summonInline[Send[h]].to(
          rawMem,
          offsets(position) + offset,
          a(position).asInstanceOf[h]
        )
        calcTupSender[t](a, rawMem, offset, offsets, position + 1)
      case _: EmptyTuple => ()

  inline def calcReceiver[A <: Product](
      offsets: Vector[Bytes]
  )(using m: Mirror.ProductOf[A]): Receive[Product] =
    (mem, offset) =>
      m.fromProduct(
        calcTupReceiver[m.MirroredElemTypes](
          mem,
          offset,
          offsets,
          0
        )
      )
  inline def calcTupReceiver[A <: Tuple](
      mem: Mem,
      offset: Bytes,
      offsets: Vector[Bytes],
      position: Int
  ): Tuple =
    inline erasedValue[A] match
      case _: (h *: t) =>
        summonInline[Receive[h]].from(
          mem,
          offset + offsets(position)
        ) *: calcTupReceiver[t](mem, offset, offsets, position + 1)
      case _: EmptyTuple =>
        EmptyTuple
package fr.hammons.sffi

import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline, constValue, codeOf}
import java.util.concurrent.atomic.AtomicReference
import scala.quoted.staging.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

class StructI(layoutI: LayoutI, exec: ExecutionContext)(using Compiler):
  import layoutI.{given, *}
  trait Struct[A] extends LayoutOf[A], Send[A]
  object Struct:
    inline def calcTupLayout[A <: Tuple]: List[DataLayout] =
      inline erasedValue[A] match
        case _: ((name, value) *: t) =>
          summonInline[LayoutOf[value]].layout
            .withName(constValue[name].toString) :: calcTupLayout[t]
        case _: EmptyTuple => Nil

    inline def derived[A <: Product, ValueTypes <: Tuple, NameTypes <: Tuple](
        using
        m: Mirror.ProductOf[A] {
          type MirroredElemLabels = NameTypes;
          type MirroredElemTypes = ValueTypes
        }
    ) = new Struct[A]:

      val useJit = Option(System.getProperty("sffi-jit"))
        .flatMap(_.nn.toBooleanOption)
        .getOrElse(true)

      val layout: StructLayout =
        structLayout(
          calcTupLayout[Tuple.Zip[NameTypes, ValueTypes]]*
        )

      private val sender: AtomicReference[Send[Product]] = AtomicReference(
        StructI.calcSender[A](layout.offsets)
      )

      def jit() = if useJit then
        given ExecutionContext = exec
        Future {
          val fn = run {
            val code = Send.sendStaged(layout)
            println(code.show)
            code
          }
          sender.lazySet(fn)
        }

      jit()

      def to(mem: Mem, offset: Bytes, a: A): Unit =
        import scala.language.unsafeNulls
        sender.get().to(mem, offset, a)
object StructI:
  inline def calcSender[A <: Product](
      offsets: Vector[Bytes]
  )(using m: Mirror.ProductOf[A]): Send[Product] =
    (rawMem: Mem, offset: Bytes, a: Product) =>
      calcTupSender[m.MirroredElemTypes](
        Tuple.fromProduct(a).toArray,
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

package fr.hammons.slinc

import scala.deriving.Mirror
import scala.compiletime.{
  uninitialized,
  erasedValue,
  summonInline,
  constValueTuple
}
import scala.reflect.ClassTag
import modules.DescriptorModule
import fr.hammons.slinc.modules.TransitionModule
import fr.hammons.slinc.modules.ReadWriteModule

class StructI(
    jitManager: JitManager
)(using DescriptorModule, TransitionModule, ReadWriteModule):
  /** Summons up Descriptors for the members of Product A
    *
    * @tparam A
    *   The product type to summon a list of descriptors for
    * @return
    *   List[TypeDescriptor]
    */
  private inline def memberDescriptors[A](using
      m: Mirror.ProductOf[A]
  ): List[TypeDescriptor] =
    memberDescriptorsHelper[m.MirroredElemTypes]
  private inline def memberDescriptorsHelper[T <: Tuple]: List[TypeDescriptor] =
    inline erasedValue[T] match
      case _: (h *: t) =>
        summonInline[DescriptorOf[h]].descriptor :: memberDescriptorsHelper[t]
      case _: EmptyTuple => Nil

  /** Summons up the names of members of the Product A
    *
    * @tparam A
    *   A product type representing a C struct.
    * @return
    */
  private inline def memberNames[A](using m: Mirror.ProductOf[A]) =
    constValueTuple[m.MirroredElemLabels].toArray.map(_.toString())

  private inline def writeGen[A <: Product](
      offsets: IArray[Bytes],
      value: A,
      mem: Mem
  )(using m: Mirror.ProductOf[A], rwm: ReadWriteModule) =
    writeGenHelper(offsets, 0, Tuple.fromProductTyped(value), mem)

  private inline def writeGenHelper[A <: Tuple](
      offsets: IArray[Bytes],
      index: Int,
      value: A,
      mem: Mem
  )(using rwm: ReadWriteModule): Unit =
    inline value match
      case tuple: (h *: t) =>
        rwm.write(mem, offsets(index), tuple.head)(using
          summonInline[DescriptorOf[h]]
        )
        writeGenHelper(offsets, index + 1, tuple.tail, mem)
      case EmptyTuple => ()

  trait Struct[A <: Product]
      extends DescriptorOf[A],
        Receive[A],
        MethodCompatible[A]

  object Struct:
    inline def derived[A <: Product](using
        m: Mirror.ProductOf[A],
        ct: ClassTag[A]
    ) = new Struct[A]:
      val descriptor: StructDescriptor = StructDescriptor(
        memberDescriptors[A].view
          .zip(memberNames[A])
          .map(StructMemberDescriptor.apply)
          .toList,
        ct.runtimeClass,
        m.fromProduct(_)
      )

      private val offsetsArray = descriptor.offsets

      // private var sender: Send[A] = uninitialized

      private var receiver: Receive[A] = uninitialized

      // jitManager.jitc(
      //   Send.compileTime[A](offsetsArray),
      //   Send.staged[A](descriptor),
      //   sender = _
      // )

      jitManager.jitc(
        Receive.compileTime[A](offsetsArray),
        Receive.staged[A](descriptor),
        receiver = _
      )

      summon[ReadWriteModule].registerWriter[A]((m, b, a) =>
        writeGen(offsetsArray.map(_ + b), a, m)
      )(using this)

      // final def to(mem: Mem, offset: Bytes, a: A): Unit =
      //   sender.to(mem, offset, a)

      final def from(mem: Mem, offset: Bytes): A =
        receiver.from(mem, offset).asInstanceOf[A]

      summon[TransitionModule].registerMethodArgumentTransition[A](
        this.descriptor,
        Allocator ?=> in(_)
      )
      summon[TransitionModule]
        .registerMethodReturnTransition[A](this.descriptor, out)
      final def in(a: A)(using alloc: Allocator): Object =
        val mem = alloc.allocate(this.descriptor, 1)
        summon[ReadWriteModule].write(mem, Bytes(0), a)(using this)
        summon[TransitionModule].methodArgument(mem).asInstanceOf[Object]

      final def out(a: Object): A =
        val mem = summon[TransitionModule].memReturn(a)
        from(mem, Bytes(0))

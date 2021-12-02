package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemoryAddress, MemoryAccess, MemorySegment}

class Ptr[+T](
    private[components] val memoryAddress: MemoryAddress
)(using template: Template[T]):

   def `unary_!` : T = template(
     memoryAddress.asSegment(template.layout.byteSize(), memoryAddress.scope)
   )

object Ptr:
   def nul = Ptr[Nothing](MemoryAddress.NULL)

   given [A](using
       template: Template[A]
   ): BoundaryCrossing[Ptr[A], MemoryAddress] with
      def toNative(ptr: Ptr[A]) = ptr.memoryAddress
      def toJVM(memoryAddress: MemoryAddress) = Ptr[A](
        memoryAddress
      )

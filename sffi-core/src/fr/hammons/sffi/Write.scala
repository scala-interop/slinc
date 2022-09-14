package fr.hammons.sffi

import fr.hammons.sffi.Write.PlatformSpecific
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.compiletime.summonInline
import scala.compiletime.codeOf
import scala.deriving.Mirror

class Write[RawMem, Context](
    platformSpecific: PlatformSpecific[RawMem, Context],
    layout: Layout[Context],
    jit: Boolean
):
  private given GenCache = GenCache( )

  // private inline def writeJitless[A](memory: RawMem, offset: Long, inline a: A) = 
  //   inline a match 
  //     case value: Int => platformSpecific.writeInt(memory, offset, value)
  //     case value: Float => platformSpecific.writeFloat(memory, offset, value)
  //     case value: (a & Product) => 
  //       val offsets = layout.offsetsOf[a & Product]
  //       (memory, offset, p) =>
  //             writeTup(
  //               memory,
  //               offset,
  //               0,
  //               offsets,
  //               Tuple.fromProductTyped(p.asInstanceOf[a & Product])(using
  //                 summonInline[Mirror.ProductOf[a & Product]]
  //               )
  //       )


  inline def write[A](memory: RawMem, offset: Long, inline a: A) =
    inline a match
      case value: Int   => platformSpecific.writeInt(memory, offset, value)
      case value: Float => platformSpecific.writeFloat(memory, offset, value)
      case product: Product =>
        GenCache.cacheForTypeJIT[A, (RawMem, Long, Product) => Unit, Write](
            //val offsets = layout.offsetsOf[A & Product]
            (memory: RawMem, offset: Long, p: Product) =>
              println("non-jit here")
              // writeTup(
              //   memory,
              //   offset,
              //   0,
              //   offsets,
              //   Tuple.fromProductTyped(p.asInstanceOf[A & Product])(using
              //     summonInline[Mirror.ProductOf[A & Product]]
              //   )
              // )
          ,platformSpecific.genStructWriter(layout.layoutOf[A])
        )(memory, offset, product)
    

  inline def writeTup[A <: Tuple](
      memory: RawMem,
      offset: Long,
      position: Int,
      offsets: IArray[Long],
      a: A
  ): Unit =
    inline a match
      case tup: (h *: t) =>
        write(memory, offset + offsets(position), tup.head)
        writeTup[t](memory, offset, position + 1, offsets, tup.tail)
      case _: EmptyTuple =>
        ()

object Write:
  trait PlatformSpecific[RawMem, Context]:
    def writeInt(memory: RawMem, offset: Long, value: Int): Unit
    def writeFloat(memory: RawMem, offset: Long, value: Float): Unit
    def writeByte(memory: RawMem, offset: Long, value: Byte): Unit
    def writeLong(memory: RawMem, offset: Long, value: Long): Unit
    def genStructWriter(
        context: Context,
    ): (RawMem, Long, Product) => Unit
// def writeIntArray(memory: RawMem, offset: Long, value: Array[Int]): Unit
// def writeFloatArray(memory: RawMem, offset: Long, value: Array[Float]): Unit

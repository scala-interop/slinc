package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.SegmentAllocator
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import scala.deriving.Mirror
import scala.CanEqual.derived
import scala.compiletime.erasedValue
import scala.compiletime.constValue
import scala.compiletime.constValueTuple
import scala.compiletime.error
import scala.compiletime.ops.any
import scala.compiletime.ops.string
import scala.compiletime.codeOf
import scala.util.chaining.*

trait Structish[T <: Product] extends HasMemoryLayout[T]:
   val layout: MemoryLayout
   val varHandles: Map[String, VarHandleHandler]
   def memSgmnt(t: T)(using m: Mirror.ProductOf[T]): MemorySegment =
      Tuple
         .fromProductTyped(t)
         .toIArray
         .view
         .collect { case f: Fd[?] =>
            f.mem
         }
         .head

   val allocate: (SegmentAllocator) => T

end Structish

object Structish:
   private inline def layoutPieces[T <: Tuple]: List[MemoryLayout] =
      inline erasedValue[T] match
         case _: ((name, FieldLike[t]) *: rest) =>
            scala2MemoryLayout[t]
               .withName(constValue[name].toString)
               .pipe(_ :: layoutPieces[rest])
         case _: EmptyTuple => Nil
         case _: ((name, t), ?) =>
            error(
              "unknown case field encountered. cannot continue processing. make sure your case class only contains members with fieldlike types"
            )

   private inline def getHandles[T <: Tuple](
       layout: MemoryLayout
   ): Map[String, VarHandleHandler] =
      inline erasedValue[T] match
         case _: ((name, FieldLike[t]) *: rest) =>
            constValue[name].toString
               .pipe(MemoryLayout.PathElement.groupElement)
               .pipe(layout.varHandle(scala2MethodTypeArg[t], _))
               .pipe(VarHandleHandler)
               .pipe(
                 getHandles[rest](layout).updated(constValue[name].toString, _)
               )
         case EmptyTuple => Map.empty

   inline given derived[T <: Product](using
       m: Mirror.ProductOf[T]
   ): Structish[T] = new Structish[T] {
      type ElemInfo = Tuple.Zip[m.MirroredElemLabels, m.MirroredElemTypes]
      println("I was created!!")
      val layout = MemoryLayout
         .structLayout(
           layoutPieces[ElemInfo]*
         )
         .withName(constValue[m.MirroredLabel].toString)
      val varHandles =
         getHandles[ElemInfo](
           layout
         )

      val allocate = (s: SegmentAllocator) =>
         s.allocate(layout)
            .pipe(genStructTup[ElemInfo](_, varHandles))
            .pipe(m.fromProduct)
   }

   private inline def genStructTup[T <: Tuple](
       memSgmnt: MemorySegment,
       varHandles: Map[String, VarHandleHandler]
   ): Tuple =
      inline erasedValue[T] match
         case _: ((name, FieldLike[t]) *: rest) =>
            Fd[t](
              memSgmnt,
              varHandles(constValue[name].toString)
            ).pipe(
              _ *: genStructTup[
                rest
              ](memSgmnt, varHandles)
            )
         case EmptyTuple => EmptyTuple
end Structish

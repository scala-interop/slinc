package io.gitlab.mhammons.slinc

import cats.free.Free
import cats.arrow.FunctionK
import cats.{Id, ~>}
import cats.catsInstancesForId
import io.gitlab.mhammons.polymorphics.VarHandleHandler
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.CLinker
import jdk.incubator.foreign.ResourceScope
import scala.util.chaining.*
import scala.quoted.*
import javax.swing.text.Segment

enum NativeOp[A]:
   case Allocate[A](
       aName: String,
       layout: MemoryLayout,
       structMaker: (MemorySegment) => A,
       allocator: SegmentAllocator
   ) extends NativeOp[A]
   case Scope[A](result: (ResourceScope, SegmentAllocator) => NativeIO[A])
       extends NativeOp[A]
   case Unit extends NativeOp[Unit]
   case Pure[A](value: () => A) extends NativeOp[A]
   case Layout(
       name: String,
       layoutGen: Map[String, MemoryLayout] => NativeIO[MemoryLayout]
   ) extends NativeOp[MemoryLayout]


type NativeIO[A] = Free[NativeOp, A]
object NativeIO:
   inline def layout[A <: StructBacking]: NativeIO[MemoryLayout] =
      Free.liftF(
        NativeOp.Layout(
          type2String[A],
          _.get(type2String[A]).map(pure(_)).getOrElse(NativePieces.deriveLayout[A])
        )
      )

   inline def allocate[A <: StructBacking](using
       SegmentAllocator
   ): NativeIO[A] =
      for
         l <- layout[A]
         r <- Free.liftF[NativeOp, A](
           NativeOp.Allocate[A](
             type2String[A],
             l,
             structFromMemorySegment[A](_),
             summon[SegmentAllocator]
           )
         )
      yield r

   inline def function[A](name: String) = ${
      NativePieces.functionImpl[A]('name)
   }

   def pure[A](a: => A) = Free.liftF(NativeOp.Pure(() => a))

   def scope[A](fn: (ResourceScope, SegmentAllocator) ?=> NativeIO[A]) =
      Free.liftF[NativeOp, A](NativeOp.Scope((r, s) => fn(using r, s)))

   val impureCompiler: NativeOp ~> Id =
      new (NativeOp ~> Id):
         var varHandles = Map.empty[String, VarHandleHandler]
         var layouts = Map.empty[String, MemoryLayout]
         val clinker = CLinker.getInstance

         def apply[A](fa: NativeOp[A]): Id[A] =
            fa match
               case NativeOp.Allocate(name, layout, structMaker, alloc) =>
                  structMaker(
                    alloc.allocate(layout)
                  )

               case NativeOp.Scope(ioFn) =>
                  val rs = ResourceScope.newConfinedScope
                  val segAlloc = SegmentAllocator
                     .arenaAllocator(rs)

                  ioFn(rs, segAlloc)
                     .foldMap(this)
                     .tap(_ => rs.close)
                     .tap(_ => println("memory freed"))

               case NativeOp.Unit =>
                  ()
               case NativeOp.Pure(aFn) => aFn()
               case NativeOp.Layout(name, gen) =>
                  gen(layouts).foldMap(this).tap(layouts += name -> _)

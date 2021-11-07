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
import java.lang.invoke.MethodHandle

type NativeIO[A] = Free[NativeOp, A]
object NativeIO:
   inline def layout[A]: NativeIO[MemoryLayout] =
      Free.liftF(
        NativePieces
           .deriveLayout[A]
           .pipe((name, layout) =>
              NativeOp
                 .Layout(
                   name,
                   m =>
                      if m.contains(name) then pure(m)
                      else layout().map(l => m + (name -> l))
                 )
           )
      )

   inline def allocate[A](using
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

   transparent inline def function[A](name: String) = ${
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
         var methodHandles = Map.empty[String, MethodHandle]

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
                  gen(layouts)
                     .foldMap(this)
                     .tap(m =>
                        if m != layouts then
                           ().tap(_ => layouts = m)
                              .tap(_ => println(s"added $name ${m(name)}"))
                     )
                     .pipe(_(name))
               case NativeOp.MethodHandleBinding(name, mhGen) =>
                  methodHandles.getOrElse(
                    name,
                    mhGen(clinker)
                       .foldMap(this)
                       .tap(mh =>
                          methodHandles = methodHandles.updated(name, mh)
                       )
                  )

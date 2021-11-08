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
import java.lang.invoke.VarHandle

type NativeIO[A] = Free[NativeOp, A]
object NativeIO:
   inline def layout[A]: NativeIO[MemoryLayout] =
      Free.liftF(
        NativePieces
           .getStructName[A]
           .pipe(name =>
              NativeOp.Layout(
                name,
                m =>
                   if m.contains(name) then pure(m)
                   else NativePieces.deriveLayout[A].map(m.updated(name, _))
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
             toStruct(_),
             summon[SegmentAllocator]
           )
         )
      yield r

   inline def toStruct[A](memorySegment: MemorySegment) =
      NativeIO
         .varHandles[A]
         .map(
           _.map((name, vh) =>
              name -> Fd(memorySegment, VarHandleHandler(vh))
           ).toMap
              .updated("$mem", memorySegment)
              .pipe(Struct(_).asInstanceOf[A])
         )
   transparent inline def function[A](name: String) = ${
      NativePieces.functionImpl[A]('name)
   }

   def pure[A](a: => A) = Free.liftF(NativeOp.Pure(() => a))

   def scope[A](fn: (ResourceScope, SegmentAllocator) ?=> NativeIO[A]) =
      Free.liftF[NativeOp, A](NativeOp.Scope((r, s) => fn(using r, s)))

   inline def varHandles[A]: NativeIO[Seq[(String, VarHandle)]] = NativePieces
      .getStructName[A]
      .pipe(name =>
         NativeOp.VarHandleBindings(
           name,
           m =>
              if m.contains(name) then pure(m)
              else NativePieces.generateVarHandles[A].map(m.updated(name, _))
         )
      )
      .pipe(Free.liftF)

   val impureCompiler: NativeOp ~> Id =
      new (NativeOp ~> Id):
         var varHandles = Map.empty[String, Seq[(String, VarHandle)]]
         var layouts = Map.empty[String, MemoryLayout]
         val clinker = CLinker.getInstance
         var methodHandles = Map.empty[String, MethodHandle]

         def apply[A](fa: NativeOp[A]): Id[A] =
            fa match
               case NativeOp.Allocate(name, layout, structMaker, alloc) =>
                  structMaker(
                    alloc.allocate(layout)
                  ).foldMap(this)

               case NativeOp.Scope(ioFn) =>
                  val rs = ResourceScope.newConfinedScope
                  val segAlloc = SegmentAllocator
                     .arenaAllocator(rs)

                  ioFn(rs, segAlloc)
                     .foldMap(this)
                     .tap(_ => rs.close)

               case NativeOp.Unit =>
                  ()
               case NativeOp.Pure(aFn) => aFn()
               case NativeOp.Layout(name, gen) =>
                  layouts
                     .getOrElse(
                       name,
                       gen(layouts).foldMap(this).tap(ls => layouts = ls)(name)
                     )
               case NativeOp.MethodHandleBinding(name, mhGen) =>
                  methodHandles.getOrElse(
                    name,
                    mhGen(clinker)
                       .foldMap(this)
                       .tap(mh =>
                          methodHandles = methodHandles.updated(name, mh)
                       )
                  )
               case NativeOp.VarHandleBindings(name, vhGen) =>
                  varHandles.getOrElse(
                    name,
                    vhGen(varHandles)
                       .foldMap(this)
                       .tap(vhs => varHandles = vhs)(name)
                  )

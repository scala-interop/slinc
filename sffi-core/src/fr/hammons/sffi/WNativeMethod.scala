package fr.hammons.sffi

import java.lang.invoke.MethodHandle
import scala.compiletime.{erasedValue, summonInline, summonFrom}

// trait WNativeMethod:
//   self: WBasics & WLayoutInfo & WInTransition & WOutTransition &
//     WStructInfo & WTempAllocator & WLookup =>

//   enum ReturnType:
//     case Unit
//     case MemorySegment(info: StructInfo[?])
//     case Standard(info: LayoutInfo[?])

  
//   class NativeMethod(methodHandle: Allocator ?=> MethodHandle):
//     type DelayedNativeCompat = Allocator ?=> NativeCompat
//     def provideAllocator(fn: Allocator ?=> (Object | Null)) = 
//       given Allocator = localAllocator()
//       fn
//     inline def apply(): Object | Null = 
//       provideAllocator(MethodHandleFacade.call(methodHandle))
//     inline def apply(a: DelayedNativeCompat) = 
//       provideAllocator(MethodHandleFacade.call(methodHandle, a))
//     inline def apply(a: DelayedNativeCompat, b: DelayedNativeCompat) =  provideAllocator(MethodHandleFacade.call(methodHandle, a, b))
//     def apply(
//         a: DelayedNativeCompat,
//         b: DelayedNativeCompat,
//         c: DelayedNativeCompat
//     ) =
//       provideAllocator(MethodHandleFacade.call(methodHandle, a, b, c))

//   def methodGen(name: String, lookup: Lookup, inputs: List[LayoutInfo[?]], ret: Option[LayoutInfo[?]]): NativeMethod =
//     val r = ret match 
//       case Some(s: StructInfo[?]) => ReturnType.MemorySegment(s)
//       case Some(l) => ReturnType.Standard(l)
//       case None => ReturnType.Unit

//     val mh = mhFromContext(inputs.map(_.context),r)
//     r match 
//       case _: ReturnType.MemorySegment => NativeMethod(mh.bindTo(summon[Allocator]).nn)
//       case _ => NativeMethod(mh)

//   protected def mhFromContext(
//       inputs: List[Context],
//       returnType: ReturnType
//   ): MethodHandle

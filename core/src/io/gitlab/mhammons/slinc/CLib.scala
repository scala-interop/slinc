package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.CLinker
import java.lang.invoke.MethodType
import jdk.incubator.foreign.FunctionDescriptor
import scala.jdk.OptionConverters.*
import scala.compiletime.erasedValue
import jdk.incubator.foreign.MemoryLayout
import jdk.incubator.foreign.SegmentAllocator
import jdk.incubator.foreign.MemorySegment

trait CLib {
  lazy val instance = CLinker.getInstance()
  def alloc = CLinker.allocateMemory(5)
  inline def fDescForRet[A](memoryLayout: MemoryLayout*) =
    inline erasedValue[A] match
      case _: Unit => FunctionDescriptor.ofVoid(memoryLayout*)
      case _ =>
        FunctionDescriptor.of(scala2MemoryLayout[A], memoryLayout*)

  inline def mTypeForRet[A] =
    inline erasedValue[A] match
      case _: Unit => VoidHelper.methodTypeV()
      case _       => MethodType.methodType(scala2MethodTypeArg[A])

  inline def mTypeForRet[A](pType0: Class[?]) =
    inline erasedValue[A] match
      case _: Unit => VoidHelper.methodTypeV(pType0)
      // case _: Struk =>
      //   MethodType.methodType(
      //     classOf[MemorySegment],
      //     classOf[SegmentAllocator],
      //     pType0
      //   )
      case _ => MethodType.methodType(scala2MethodTypeArg[A], pType0)

  inline def mTypeForRet[A](pType0: Class[?], ptypes: Class[?]*) =
    inline erasedValue[A] match
      case _: Unit => VoidHelper.methodTypeV(pType0, ptypes*)
      case _ => MethodType.methodType(scala2MethodTypeArg[A], pType0, ptypes*)
  inline def downcall[A](name: String) =
    CLinker.systemLookup
      .lookup(name)
      .toScala
      .map(
        instance.downcallHandle(
          _,
          mTypeForRet[A],
          fDescForRet[A]()
        )
      )
      .map(mh => () => returnType[A](mh.invokeWithArguments()))

  inline def downcall[A, B](name: String) = CLinker.systemLookup
    .lookup(name)
    .toScala
    .map(
      instance.downcallHandle(
        _,
        mTypeForRet[B](scala2MethodTypeArg[A]),
        fDescForRet[B](scala2MemoryLayout[A])
      )
    )
    .map(mh => (a: A) => mh.invokeWithArguments(a).asInstanceOf[B])

  inline def downcallS[A, B](name: String) = CLinker.systemLookup
    .lookup(name)
    .toScala
    .map(
      instance.downcallHandle(
        _,
        mTypeForRet[B](scala2MethodTypeArg[A]),
        fDescForRet[B](scala2MemoryLayout[A])
      )
    )
    .map(mh =>
      (sG: SegmentAllocator) ?=>
        (a: A) => mh.invokeWithArguments(sG, a).asInstanceOf[MemorySegment]
    )

  inline def downcallS[A, B, C](name: String) = CLinker.systemLookup
    .lookup(name)
    .toScala
    .map(
      instance.downcallHandle(
        _,
        mTypeForRet[C](scala2MethodTypeArg[A], scala2MethodTypeArg[B]),
        fDescForRet[C](scala2MemoryLayout[A], scala2MemoryLayout[B])
      )
    )
    .map(mh =>
      (sG: SegmentAllocator) ?=>
        (a: A, b: B) =>
          mh.invokeWithArguments(sG, a, b).asInstanceOf[MemorySegment]
    )
}

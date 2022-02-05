package io.gitlab.mhammons.slinc.components

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.compiletime.summonFrom
import io.gitlab.mhammons.polymorphics.VoidHelper
import jdk.incubator.foreign.CLinker.C_POINTER
import jdk.incubator.foreign.{MemoryAddress, FunctionDescriptor}

trait Callback[A] extends Writer[A], NativeInfo[A]

object Callback:
   inline given [A](using NativeInfo[A]): Callback[Function0[A]] =
      new Callback[Function0[A]]:
         val carrierType = classOf[MemoryAddress]
         val layout = C_POINTER

         def into(a: Function0[A], addr: MemoryAddress, offset: Long) =
            val methodType = summonFrom {
               case _: =:=[A, Unit] =>
                  VoidHelper.methodTypeV
               case _ => MethodType.methodType(infoOf[A].carrierType)
            }
            val functionDescriptor = summonFrom {
               case _: =:=[A, Unit] => FunctionDescriptor.ofVoid()
               case _               => FunctionDescriptor.of(infoOf[A].layout)
            }
            val lambdaMh = MethodHandles.lookup
               .findVirtual(
                 classOf[Function0[?]],
                 "apply",
                 MethodType.genericMethodType(0)
               )
               .bindTo(a)
               .asType(methodType)

            Linker.linker.upcallStub(lambdaMh, functionDescriptor, addr.scope)

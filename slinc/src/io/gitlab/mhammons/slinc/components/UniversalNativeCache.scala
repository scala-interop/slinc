package io.gitlab.mhammons.slinc.components

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap
import java.lang.invoke.MethodHandle
import jdk.incubator.foreign.CLinker

object UniversalNativeCache:
   val bindingsIndexes = AtomicInteger(0)
   val seenBindings = TrieMap.empty[String, Int]
   def getBindingIndex(name: String) =
      seenBindings.getOrElseUpdate(name, bindingsIndexes.getAndIncrement)

   private val methodHandles = GrowingArray[MethodHandle](64)

   inline def addMethodHandle(
       index: Int,
       inline mh: MethodHandle
   ): MethodHandle =
      methodHandles.get(index, mh)

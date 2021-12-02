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

   val layoutIndexes = AtomicInteger(0)
   val seenLayouts = TrieMap.empty[String, Int]
   def getLayoutIndex(name: String) =
      seenLayouts.getOrElseUpdate(name, layoutIndexes.getAndIncrement)

   val structParams = TrieMap.empty[String, Map[String, Int]]

   def addStructParams(structName: String, names: List[String]) =
      structParams.addOne(structName -> names.zipWithIndex.toMap)

   def getIndex(structName: String, paramName: String) =
      structParams(structName)(paramName)

   private val methodHandles = GrowingArray[MethodHandle](64)

   inline def addMethodHandle(
       index: Int,
       inline mh: MethodHandle
   ): MethodHandle =
      methodHandles.get(index, mh)

   private val templates = GrowingArray[Template[?]](1)
   private val boundaryCrossings = GrowingArray[BoundaryCrossing[?, ?]](1)
   private val layouts = GrowingArray[LayoutOf[?]](1)

   inline def getTemplate[A](
       index: Int,
       inline template: Template[A]
   ): Template[A] = templates.get(index, template).asInstanceOf[Template[A]]

   inline def getBoundaryCrossing[A, B](
       index: Int,
       inline boundaryCrossing: BoundaryCrossing[A, B]
   ): BoundaryCrossing[A, B] = boundaryCrossings
      .get(index, boundaryCrossing)
      .asInstanceOf[BoundaryCrossing[A, B]]

   inline def getLayout[A](
       index: Int,
       inline layout: LayoutOf[A]
   ): LayoutOf[A] = layouts.get(index, layout).asInstanceOf[LayoutOf[A]]

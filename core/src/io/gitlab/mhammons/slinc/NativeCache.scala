package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.{MemoryLayout, CLinker}
import scala.util.chaining.*
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.Map
import java.lang.invoke.VarHandle
import java.lang.invoke.MethodHandle
import io.gitlab.mhammons.slinc.components.MemLayout

//todo: make NativeCache just a trait...
class NativeCache:
   private val layouts = TrieMap.empty[String, MemoryLayout]
   private val varHandlesMap = TrieMap.empty[String, List[(String, VarHandle)]]
   private val methodHandles = TrieMap.empty[String, MethodHandle]

   inline def layout[A]: MemoryLayout =
      given NativeCache = this

      val name = LayoutMacros.layoutName[A]
      layouts
         .get(name)
         .getOrElse(LayoutMacros.deriveLayout[A].tap(layouts.update(name, _)))

   inline def layout2[A]: MemLayout =
      given NativeCache = this

      LayoutMacros.deriveLayout2[A]

   inline def varHandles[A]: List[(String, VarHandle)] =
      varHandlesMap.getOrElseUpdate(
        LayoutMacros.layoutName[A],
        StructMacros.genVarHandles[A].toList
      )

   inline def downcall(name: String, mh: => MethodHandle) =
      methodHandles.getOrElseUpdate(name, mh)

   val clinker = CLinker.getInstance

object NativeCache
//given NativeCache = NativeCache()

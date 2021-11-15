package io.gitlab.mhammons.slinc

import jdk.incubator.foreign.{MemoryLayout, CLinker}
import scala.util.chaining.*
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.Map
import java.lang.invoke.VarHandle
import java.lang.invoke.MethodHandle
import io.gitlab.mhammons.slinc.components.MemLayout

//todo: make NativeCache just a trait...

trait NativeCache:
   def getLayout(name: String, layout: => MemLayout): MemLayout
   def getVarHandles(
       name: String,
       varHandles: => List[(String, VarHandle)]
   ): List[(String, VarHandle)]
   def getDowncall(name: String, mh: => MethodHandle): MethodHandle
   inline def layout[A]: MemLayout =
      getLayout(LayoutMacros.layoutName[A], LayoutMacros.deriveLayout2[A])
   inline def varHandles[A]: List[(String, VarHandle)] =
      getVarHandles(
        LayoutMacros.layoutName[A],
        StructMacros.genVarHandles[A].toList
      )
   def downcall(name: String, mh: => MethodHandle): MethodHandle =
      getDowncall(name, mh)
   val clinker: CLinker

class NativeCacheDefaultImpl extends NativeCache:
   private[slinc] val layouts = TrieMap.empty[String, MemLayout]
   private[slinc] val varHandlesMap =
      TrieMap.empty[String, List[(String, VarHandle)]]
   private[slinc] val methodHandles = TrieMap.empty[String, MethodHandle]

   def getDowncall(name: String, mh: => MethodHandle) =
      methodHandles.getOrElseUpdate(name, mh)
   def getLayout(name: String, layout: => MemLayout) =
      layouts.getOrElseUpdate(name, layout)

   def getVarHandles(
       name: String,
       varHandles: => List[(String, VarHandle)]
   ) =
      varHandlesMap.getOrElseUpdate(name, varHandles)

   val clinker = CLinker.getInstance

object NativeCache
//given NativeCache = NativeCache()

package fr.hammons.slinc.modules

import scala.collection.concurrent.TrieMap
import fr.hammons.slinc.TypeDescriptor

class DependentTrieMap[F[_]]:
  val cache: TrieMap[TypeDescriptor, Any] = TrieMap.empty
  def getOrElseUpdate(t: TypeDescriptor, fn: => F[t.Inner]): F[t.Inner] =
    cache.getOrElseUpdate(t, fn).asInstanceOf[F[t.Inner]]

  def addOne(t: TypeDescriptor, fn: => F[t.Inner]): Unit =
    cache.update(t, fn)

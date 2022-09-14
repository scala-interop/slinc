package fr.hammons.sffi

import scala.reflect.ClassTag
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import scala.quoted.*
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Vector
import scala.util.chaining.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GenCache():
  val jit = Option(System.getProperty("enable-sffi-jit")).flatMap(_.nn.toBooleanOption).getOrElse(true)
  val store2 = AtomicReferenceArray[AtomicReferenceArray[Object]](27)
    .tap(_.set(0, AtomicReferenceArray[Object](sizeOf(0))))

  println("jit enabled: " + jit)
  // val sizes = Array.iterate(16, 27)(_ << 2)

  inline val baseStorage = 16
  inline def totalSize(inline groupNum: Int): Int = inline if groupNum == 0 then
    sizeOf(groupNum)
  else sizeOf(groupNum) + totalSize(groupNum - 1)

  inline def sizeOf(inline groupNum: Int): Int = baseStorage << groupNum
  inline def groupOf(inline idx: Int, inline guess: Int = 0): Int =
    inline if idx < totalSize(guess) then guess else groupOf(idx, guess + 1)
  inline def modifiedIndex(inline idx: Int): Int =
    inline if groupOf(idx) == 0 then idx else idx - totalSize(groupOf(idx) - 1)

  private def cacheValue[T](
      array: AtomicReferenceArray[Object],
      idx: Int,
      t: T
  ) =
    array.compareAndSet(idx, null, t.asInstanceOf[Object])
    t

  inline def getOrAdd2[T](inline idx: Int, inline t: T): T =
    val array = store2.get(groupOf(idx))

    if array != null then
      val cachedValue = array.get(modifiedIndex(idx))
      if cachedValue == null then cacheValue(array, modifiedIndex(idx), t)
      else cachedValue.asInstanceOf[T]
    else
      val newSection = AtomicReferenceArray[Object](sizeOf(groupOf(idx)))
      if store2.compareAndSet(groupOf(idx), null, newSection) then
        cacheValue(newSection, modifiedIndex(idx), t)
      else cacheValue(store2.get(groupOf(idx)).nn, modifiedIndex(idx), t)

  inline def getOrAddJit[T](inline idx: Int, inline t: T, inline fT: T): T =
    val array = store2.get(groupOf(idx))

    if array == null then 
      val newSection = AtomicReferenceArray[Object](sizeOf(groupOf(idx)))
      if store2.compareAndSet(groupOf(idx), null, newSection) then 
        cacheValue(newSection, modifiedIndex(idx), t)
      else cacheValue(store2.get(groupOf(idx)).nn, modifiedIndex(idx), t)
    else 
      val cachedValue = array.get(modifiedIndex(idx))
      if cachedValue == null then
        //array.compareAndSet(modifiedIndex(idx),null, ().asInstanceOf[Object])
        val unjitted = t
        println(idx)
        array.compareAndSet(
          modifiedIndex(idx),
          null,
          unjitted.asInstanceOf[Object]
        )
        if jit then 
          Future(fT).foreach(jitted =>
            array.lazySet(modifiedIndex(idx), jitted.asInstanceOf[Object])
          )
        unjitted
      else cachedValue.asInstanceOf[T]

  val store =
    AtomicReference[AtomicReferenceArray[Object]](AtomicReferenceArray(1024))

  inline def getOrAdd[T](idx: Int, inline t: T) =
    val array = store.get().nn
    val tooSmall = array.length < idx

    if tooSmall then
      val newArray =
        Array.ofDim[Object](array.length * 2).asInstanceOf[Array[Object | Null]]
      Array.copy(array, 0, newArray, 0, array.length)
      val newAtomicArray = AtomicReferenceArray.apply(newArray)
      store.compareAndSet(array, newAtomicArray)
      val res = t
      newAtomicArray.compareAndSet(idx, null, res.asInstanceOf[Object])
      res.asInstanceOf[T]
    else
      val value = array.get(idx)
      if value == null then
        val res = t
        array.compareAndSet(idx, null, res.asInstanceOf[Object])
        res.asInstanceOf[T]
      else value.asInstanceOf[T]

// inline private def update(idx: Int, t: Object) =
//   val array = store.get().nn
//   array.lazySet(idx,t)
//   store.compareAndSet(array, array.lazySet(idx, t))

object GenCache:
  inline def cacheForType[A, B, Container <: AnyKind](inline value: B)(using GenCache) = ${
    cacheForTypeImpl[A, B, Container]('{ summon[GenCache] }, 'value)
  }

  inline def cacheForTypeJIT[A, B, Container <: AnyKind](inline unjit: B, inline jit: B)(using
      GenCache,
      ExecutionContext
  ) = ${
    cacheForTypeJITImpl[A, B, Container](
      '{ summon[GenCache] },
      'unjit,
      'jit
    )
  }

  private val indexMap = TrieMap.empty[(String, String), Int]
  private val nextIndex = TrieMap.empty[String, Int]

  private def cacheForTypeImpl[A, B, Container <: AnyKind](
      genCache: Expr[GenCache],
      valueExpr: Expr[B]
  )(using Quotes, Type[A], Type[B], Type[Container]) =
    import quotes.reflect.*
    val index = getCacheIndex[A, Container]
    '{ $genCache.getOrAdd2(${ Expr(index) }, $valueExpr) }

  private def cacheForTypeJITImpl[A, B, Container <:AnyKind](
      genCache: Expr[GenCache],
      unjitExpr: Expr[B],
      jitExpr: Expr[B]
  )(using Quotes, Type[A], Type[B], Type[Container]) =
    import quotes.reflect.*
    val index = 0
    
    val code = '{
      $genCache.getOrAddJit(${ Expr(index) }, {
      $unjitExpr}, $jitExpr)
    }
    //report.errorAndAbort(jitExpr.show)
    //report.errorAndAbort(unjitExpr.show)
    code

  private def getCacheIndex[A, Container <: AnyKind](using Quotes, Type[A], Type[Container]) =
    import quotes.reflect.*
    val cacheLoc = Type.show[Container]
    val typeName = TypeRepr.of[A].show(using Printer.TypeReprCode)

    indexMap.get((cacheLoc, typeName)) match
      case Some(index) =>
        index
      case None =>
        val index = nextIndex.getOrElseUpdate(cacheLoc, 0)
        nextIndex.update(cacheLoc, index + 1)
        indexMap.update((cacheLoc, typeName), index)
        index

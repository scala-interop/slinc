package io.gitlab.mhammons.slinc.benches

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import io.gitlab.mhammons.slinc.components.LRU
import scala.util.Random

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class LRUBench:
   @Param(Array("100", "300", "500"))
   var n: Int = _
   var lru: LRU = _

   @Param(Array(".05", ".15", ".25"))
   var randomness: Double = _

   class KeyStream(randomness: Double, lruSize: Int):
      val keys =
         List.fill(Math.round(lruSize * 1.05).toInt)(Random.nextInt().toString)
      val stream =
         LazyList
            .iterate(Random.shuffle(keys).head)(a =>
               if Random.nextDouble < randomness then Random.shuffle(keys).head
               else a
            )
            .iterator
      var next: String = _
      def loadNext() = next = stream.next
      def getNext = next

   var keyStream: KeyStream = _

   @Setup(Level.Trial)
   def setupLRU() =
      lru = new LRU(n)
      keyStream = KeyStream(randomness, n)
      keyStream.loadNext()

   var purelyRandomKey: String = _
   @Setup(Level.Invocation)
   def loadNext() =
      keyStream.loadNext()
      purelyRandomKey = Random.nextInt().toString

   val nonRandomKey = Random.nextInt().toString

   // @TearDown(Level.Trial)
   // def printDiagnostics =
   //    lru.printDiagnostics()

   def genObject() =
      Thread.sleep(0, 100_000)
      Object()

   @Benchmark
   def lruNonRandom = lru.get(nonRandomKey, genObject())

   @Benchmark
   def lruSemiRandom = lru.get(keyStream.getNext, genObject())

   @Benchmark
   def lruTrulyRandom = lru.get(purelyRandomKey, genObject())

package io.gitlab.mhammons.slinc.components

import jdk.incubator.foreign.{MemoryAddress, MemorySegment, SegmentAllocator, CLinker}
import io.gitlab.mhammons.slinc.Struckt
import scala.quoted.*

trait BoundaryCrossing[A, B]:
   def toNative(a: A): B
   def toJVM(b: B): A
   type Native = B

object BoundaryCrossing:
   given BoundaryCrossing[Int, Int] with
      def toNative(int: Int) = int
      def toJVM(int: Int) = int

   given BoundaryCrossing[Float, Float] with
      def toNative(float: Float) = float
      def toJVM(float: Float) = float

   given BoundaryCrossing[Double, Double] with
      def toNative(double: Double) = double
      def toJVM(double: Double) = double

   given BoundaryCrossing[Long, Long] with
      def toNative(long: Long) = long
      def toJVM(long: Long) = long


   given (using seg: SegmentAllocator): BoundaryCrossing[String, MemoryAddress] 
      with
      def toNative(string: String) = CLinker.toCString(string, seg).address
      def toJVM(memoryAddress: MemoryAddress) =
         CLinker.toJavaString(memoryAddress)

   given [A <: Product](using SegmentAllocator)(using struckt: Struckt[A]): BoundaryCrossing[A, MemorySegment] with
      def toNative(a: A) = struckt.to(a).asMemorySegment
      def toJVM(memorySegment: MemorySegment) = struckt.from(memorySegment, 0)

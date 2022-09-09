package fr.hammons.sffi

import scala.compiletime.{erasedValue, constValue}
import scala.compiletime.{error, codeOf}
import scala.compiletime.ops.int.{+, `*`, `-`, S}
import scala.reflect.ClassTag

trait StaticArrayI {
  self: WBasics & WLayoutInfo =>

  type ReduceTowardsZero[T] <: Int = T match 
    case 0 => 0
    case S[t] => t 
  class StaticArray[A, Size <: Int](private val array: Array[A], private val offset: Int)(using LayoutInfo[A], ClassTag[A]):
    def map[B](fn: A => B)(using ClassTag[B], LayoutInfo[B]) = StaticArray[B,Size](array.map(fn), offset)
    def ++[NSize <: Int](o: StaticArray[A,NSize]): StaticArray[A, Size + NSize] = StaticArray[A,Size+NSize](array ++ o.array, offset + o.offset)
    inline def head: A = array(0 + offset)
    inline def tail: StaticArray[A,ReduceTowardsZero[Size]] = 
      inline erasedValue[Size] match 
        case 0 => 
          inline StaticArray[A,0](array, offset) match 
            case r: StaticArray[A,ReduceTowardsZero[Size]] => r
        case _ =>
          StaticArray[A,ReduceTowardsZero[Size]](array,offset)
    inline def isEmpty: Boolean = inline erasedValue[Size] match 
      case 0 => true 
      case _ => false
    
    inline def foldLeft[B](inline b: B, inline fn: (B, A) => B): B = 
      inline if isEmpty then b else tail.foldLeft(fn(b, head), fn)
    inline def flatMap[B, NSize <: Int](fn: A => StaticArray[B,NSize]): StaticArray[B,NSize*Size] = ???
  

  extension [A](a: Array[A])(using LayoutInfo[A], ClassTag[A])
    inline def toStatic[Size <: Int] = 
      val declaredSize = constValue[Size]
      if declaredSize != a.length then 
        throw new Exception("Invalid array size")
      else 
        StaticArray[A,Size](a, 0)


  object StaticArray:
    inline def ofDim[A,Size <: Int](using LayoutInfo[A], ClassTag[A]) = 
      StaticArray[A,Size](Array.ofDim(constValue[Size]), 0)
    inline def fill[A, Size <: Int](inline a: A)(using LayoutInfo[A], ClassTag[A]) = ???

  given sa[A,Size <: Int](using ValueOf[Size], LayoutInfo[A]): LayoutInfo[StaticArray[A,Size]]

}

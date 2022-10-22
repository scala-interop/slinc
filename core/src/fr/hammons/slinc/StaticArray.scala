package fr.hammons.slinc

import scala.reflect.ClassTag
import scala.compiletime.constValue

class StaticArray[A <: Int, B](a: Array[B])(using ClassTag[B])

object StaticArray:
  inline def ofDim[A, Size <: Int](using ClassTag[A]) = StaticArray(
    Array.ofDim[A](constValue[Size])
  )

  

package fr.hammons.sffi

import scala.compiletime.ops.int.+
import scala.compiletime.ops.boolean.&&

type IndexOf[S <: Singleton & String, T <: Tuple] <: Int = T match
  case (S, ?) *: ? => 0
  case ? *: tail   => IndexOf[S, tail] + 1

type KeyExists[S <: Singleton & String, T <: Tuple] <: Boolean = T match 
  case (S, ?) *: ? => true 
  case ? *: tail => KeyExists[S,tail]
  case EmptyTuple => false

type Values[T <: Tuple] <: Tuple = T match
  case (?, v) *: tail => v *: Values[tail]
  case EmptyTuple     => EmptyTuple

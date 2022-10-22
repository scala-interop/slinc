package fr.hammons.slinc

import scala.compiletime.ops.int.{S, +}
import scala.compiletime.constValue
import scala.compiletime.summonAll
import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.util.NotGiven
import scala.collection.immutable.LazyList.cons
import scala.compiletime.codeOf
import scala.quoted.*

//opaque type ContextProof[C <: Capabilities, A] = ContextProof.ToTuple[C, A]
class ContextProof[C <: Capabilities, A](val tup: ContextProof.ToTuple[C, A])

object ContextProof:
  inline def apply[C <: Capabilities, A](): ContextProof[C, A] =
    new ContextProof(summonAll[ToTuple[C, A]])

  type ToTuple[A <: Capabilities, T] <: Tuple = A match
    case head *::: tail => head[T] *: ToTuple[tail, T]
    case End            => EmptyTuple

  type ToUnion[A <: Capabilities, B] = A match
    case head *::: tail => head[B] | ToUnion[tail, B]
    case End            => Nothing

  type ToSum[A <: Capabilities, B] = A match
    case head *::: tail => head[B] & ToSum[tail, B]
    case End            => Nothing

  inline def fetchable[C[_], A <: Capabilities]: Boolean =
    inline erasedValue[A] match
      case _: (C *::: ?)    => true
      case _: (? *::: rest) => fetchable[C, rest]
      case _: End           => false

  inline def fetchIdx[C[_], A <: Capabilities]: Int =
    inline erasedValue[A] match
      case _: (C *::: ?)    => 0
      case _: (? *::: rest) => fetchIdx[C, rest] + 1
      case _: End           => error("ended")

  type IndexOf[Caps, A[_]] <: Int = Caps match
    case A *::: rest => 0
    case ? *::: rest => 1 + IndexOf[rest, A]
    case End         => -1155213512

  inline given [A, Cap <: Capabilities, N <: Int](using c: ContextProof[Cap,A], l: LocationInCap[LayoutOf, Cap, N]): LayoutOf[A] = c.tup.productElement(constValue[N]).asInstanceOf[LayoutOf[A]]

  inline given [A, Cap <: Capabilities, N <: Int](using c: ContextProof[Cap, A], l: LocationInCap[NativeInCompatible,Cap,N]): NativeInCompatible[A] = c.tup.productElement(constValue[N]).asInstanceOf[NativeInCompatible[A]]

  inline given [A, Cap <: Capabilities, N <: Int](using c: ContextProof[Cap, A], l: LocationInCap[Send, Cap, N]): Send[A] = c.tup.productElement(constValue[N]).asInstanceOf[Send[A]]

  inline given [A, Cap <: Capabilities, N <: Int](using c: ContextProof[Cap, A], l: LocationInCap[Receive, Cap, N]): Receive[A] = c.tup.productElement(constValue[N]).asInstanceOf[Receive[A]]

  // inline given [A, Cap <: Capabilities, B[A] >: ToSum[Cap,A], N <: Int](using c: => ContextProof[Cap,A], l: => LocationInCap[B,Cap,N])(using ToSum[Cap,A] <:< B[A]): B[A] = ???

  // transparent inline given [A, Cap <: Capabilities](using
  //     c: ContextProof[Cap, A]
  // ): LayoutOf[A] = c.get[LayoutOf]

  // transparent inline given [A, Cap <: Capabilities](using
  //     c: ContextProof[Cap, A]
  // ): NativeInCompatible[A] = c.get[NativeInCompatible]

  // transparent inline given [A, Cap <: Capabilities](using c: ContextProof[Cap,A]): Send[A] = c.get[Send]

  // transparent inline given [A, Cap <: Capabilities](using c: ContextProof[Cap, A]): Receive[A] = c.get[Receive]
  // inline def apply[A] =
  //   const

  // given [A,B[_], Rest <: Capabilities](using ContextProof[])

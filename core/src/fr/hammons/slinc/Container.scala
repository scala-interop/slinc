package fr.hammons.slinc

import scala.reflect.ClassTag
import scala.util.Try
import scala.util.Success
import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.quoted.*

sealed trait Capabilities
sealed trait *:::[A[_], B <: Capabilities] extends Capabilities
sealed trait End extends Capabilities

class Data[A](a: A):
  type B = A
  val b: B = a

class Use[A[_]](val b: Data[?])(using ev: A[b.B]):
  def apply[C](fn: A[b.B] ?=> b.B => C): C = fn(b.b)

class Container[A <: Capabilities](
    val data: Data[?],
    val evidences: Array[AnyRef]
):
  inline def fetchIdx[C[_], A <: Capabilities]: Int =
    inline erasedValue[A] match
      case _: (C *::: ?)    => 0
      case _: (? *::: rest) => fetchIdx[C, rest] + 1
      case _: End           => error("ended")

  inline def fetchable[C[_], A <: Capabilities]: Boolean =
    inline erasedValue[A] match
      case _: (C *::: ?)    => true
      case _: (? *::: rest) => fetchable[C, rest]
      case _: End           => false

  inline def use[C[_]]: Use[C] =
    inline if fetchable[C, A] then
      Use(data)(using evidences(fetchIdx[C, A]).asInstanceOf[C[data.B]])
    else error("The requisite capability doesn't exist")

object Container:
  type ToTuple[A <: Capabilities, T] <: Tuple = A match
    case head *::: tail => head[T] *: ToTuple[tail, T]
    case End            => EmptyTuple

  inline def apply[A <: Capabilities](data: Any): Container[A] = ${
    applyImpl[A]('data)
  }

  inline implicit def getContainer[A <: Capabilities](
      data: Any
  ): Container[A] =
    ${
      applyImpl[A]('data)
    }

  private def getEvidences[A <: Tuple](using
      Type[A],
      Quotes
  ): Expr[Vector[AnyRef]] =
    import quotes.reflect.*
    Type.of[A] match
      case '[head *: tail] =>
        val ev = Expr
          .summon[head]
          .getOrElse(
            report.errorAndAbort(
              s"Couldn't find ${Type.show[head]} in scope"
            )
          )
          .asExprOf[AnyRef]
        var rest = getEvidences[tail]
        '{
          $ev +: $rest
        }
      case '[EmptyTuple] =>
        '{ Vector.empty }

  private def applyImpl[A <: Capabilities](
      data: Expr[Any]
  )(using Quotes, Type[A]) =
    import quotes.reflect.*

    data match
      case '{ $a: i } =>
        TypeRepr.of[i].widen.asType match
          case '[j] =>
            val expr = getEvidences[ToTuple[A, j]]
            '{
              new Container[A](Data($data), ${ expr }.toArray)
            }
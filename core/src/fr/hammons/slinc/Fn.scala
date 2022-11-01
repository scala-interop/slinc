package fr.hammons.slinc

import scala.annotation.targetName
import scala.quoted.*
import scala.util.TupledFunction
import scala.annotation.experimental
import scala.util.chaining.*


trait Fn[F, Inputs <: Tuple, Output]:
  type Function = F
  // val eq: =:=[F, FnCalc[Inputs, Output]]
  def andThen(fn: Function, andThen: Output => Output): Function

object Fn:
  type NativeOut[A] = A match
    case Int    => Int
    case Float  => Float
    case Long   => Long
    case Double => Double
    case Short  => Short
    case Byte   => Byte
    case Unit   => Unit
    case _      => Object

  transparent inline def toNativeCompatible[A](inline a: A) = ${
    toNativeCompatibleImpl('a)
  }

  def toNativeCompatibleImpl[A](a: Expr[A])(using Quotes, Type[A]) =
    import quotes.reflect.*
    val typeArgs = TypeRepr.of[A].typeArgs
    val inputTypes = typeArgs.init
    val outputType = typeArgs.last
    val names = LazyList.continually("zzzzz")
    val select =
      Select(a.asTerm, TypeRepr.of[A].typeSymbol.declaredMethod("apply").head)
    val lambda = Lambda(
      Symbol.spliceOwner,
      MethodType(names.take(typeArgs.size - 1).toList)(
        _ => inputTypes.map(TypeRepr.of[NativeOut].appliedTo(_).simplified),
        _ => TypeRepr.of[NativeOut].appliedTo(outputType).simplified
      ),
      (meth, params) =>
        params
          .map(_.asExpr)
          .zip(inputTypes.map(_.asType))
          .map { case (p, '[a]) =>
            NativeOutCompatible.handleOutput[a](p).asTerm
          }
          .pipe { terms =>
            outputType.asType match
              case '[r] =>
                NativeInCompatible
                  .handleInput(
                    Expr.betaReduce(Apply(select, terms).asExprOf[r])
                  )
                  .asTerm
                  .changeOwner(meth)
          }
    ).asExpr

    Expr.betaReduce(lambda)

  given [Z]: Fn[() => Z, EmptyTuple, Z] with
    // val eq = summon[(() => Z) =:= FnCalc[EmptyTuple, Z]]
    def andThen(fn: () => Z, andThen: Z => Z): Function = () => andThen(fn())

  given [A, Z]: Fn[A => Z, Tuple1[A], Z] with
    def andThen(fn: A => Z, andThen: Z => Z): Function = (a: A) =>
      andThen(fn(a))
    // val eq = summon[(Function) =:= FnCalc[Tuple1[A],Z]]

  given [A, B, Z]: Fn[(A, B) => Z, (A, B), Z] with
    def andThen(fn: (A, B) => Z, andThen: Z => Z): Function = (a: A, b: B) =>
      andThen(fn(a, b))
    // val eq = summon[(Function) =:= FnCalc[(A,B), Z]]

  given [A, B, C, Z]: Fn[(A, B, C) => Z, (A, B, C), Z] with
    def andThen(fn: (A, B, C) => Z, andThen: Z => Z): Function =
      (a: A, b: B, c: C) => andThen(fn(a, b, c))
    // val eq = summon[(Function) =:= FnCalc[(A,B,C),Z]]

  extension [F, Input <: Tuple, Output](fn: F)(using ftc: Fn[F, Input, Output])
    def andThen(fn2: Output => Output): F = ftc.andThen(fn, fn2)

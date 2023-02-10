package fr.hammons.slinc

import scala.quoted.*
import scala.util.chaining.*
import fr.hammons.slinc.modules.TransitionModule
import scala.annotation.nowarn

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

  // todo: remove once https://github.com/lampepfl/dotty/issues/16863 is fixed
  @nowarn("msg=unused implicit parameter")
  @nowarn("msg=unused local definition")
  def toNativeCompatibleImpl[A](a: Expr[A])(using Quotes, Type[A]): Expr[A] =
    import quotes.reflect.*
    val typeArgs = TypeRepr.of[A].typeArgs
    val inputTypes = typeArgs.init
    val outputType = typeArgs.last
    val names = LazyList.continually("zzzzz")
    val tm = Expr
      .summon[TransitionModule]
      .getOrElse(report.errorAndAbort(s"Need transition module!"))
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
            val desc = Expr
              .summon[DescriptorOf[a]]
              .getOrElse(
                report.errorAndAbort(
                  s"Could not find descriptor for ${Type.show[a]}"
                )
              )
            '{
              $tm.functionArgument[a]($desc.descriptor, $p.asInstanceOf[Object])
            }.asTerm
          }
          .pipe { terms =>
            outputType.asType match
              case '[Unit] =>
                '{
                  ${ Expr.betaReduce(Apply(select, terms).asExpr) }
                  ()
                }.asTerm.changeOwner(meth)
              case '[r] =>
                val desc = Expr
                  .summon[DescriptorOf[r]]
                  .getOrElse(
                    report.errorAndAbort(
                      s"Couldn't find descriptor for ${Type.show[r]}"
                    )
                  )
                '{
                  $tm
                    .functionReturn[r](
                      $desc.descriptor,
                      ${ Expr.betaReduce(Apply(select, terms).asExprOf[r]) },
                      null.asInstanceOf[Allocator]
                    )
                    .asInstanceOf[r]
                }.asTerm.changeOwner(meth)
          }
    ).asExprOf[A]

    report.info(lambda.show)

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

package fr.hammons.sffi

import scala.annotation.targetName

type FnCalc[Inputs <: Tuple, Output] = Inputs match
  case EmptyTuple => () => Output
  case Tuple1[a]  => a => Output
  case (a, b)     => (a, b) => Output
  case (a, b, c)  => (a, b, c) => Output

trait Fn[F, Inputs <: Tuple, Output]:
  type Function = F
  //val eq: =:=[F, FnCalc[Inputs, Output]]
  def andThen(fn: Function, andThen: Output => Output): Function
  @targetName("complexAndThen")
  def andThen[ZZ](fn: Function, andThen: Output => ZZ): FnCalc[Inputs, ZZ]

object Fn:
  given [Z]: Fn[() => Z, EmptyTuple, Z] with
    //val eq = summon[(() => Z) =:= FnCalc[EmptyTuple, Z]]
    def andThen(fn: () => Z, andThen: Z => Z): Function = () => andThen(fn())
    @targetName("complexAndThen")
    def andThen[ZZ](fn: Function, andThen: Z => ZZ) = () => andThen(fn())

  given [A, Z]: Fn[A => Z, Tuple1[A], Z] with
    def andThen(fn: A => Z, andThen: Z => Z): Function = (a: A) => andThen(fn(a))
    //val eq = summon[(Function) =:= FnCalc[Tuple1[A],Z]]
    @targetName("complexAndThen")
    def andThen[ZZ](fn: Function, andThen: Z => ZZ) = (a: A) => andThen(fn(a))

  given [A, B, Z]: Fn[(A, B) => Z, (A, B), Z] with
    def andThen(fn: (A, B) => Z, andThen: Z => Z): Function = (a: A, b: B) => andThen(fn(a, b))
    //val eq = summon[(Function) =:= FnCalc[(A,B), Z]]
    @targetName("complexAndThen")
    def andThen[ZZ](fn: Function, andThen: Z => ZZ) = (a: A, b: B) =>
      andThen(fn(a, b))

  given [A, B, C, Z]: Fn[(A, B, C) => Z, (A, B, C), Z] with
    def andThen(fn: (A, B, C) => Z, andThen: Z => Z): Function = (a: A, b: B, c: C) => andThen(fn(a,b,c))
    //val eq = summon[(Function) =:= FnCalc[(A,B,C),Z]]
    @targetName("complexAndThen")
    def andThen[ZZ](fn: Function, andThen: Z => ZZ) = (a: A, b: B, c: C) =>
      andThen(fn(a, b, c))

  extension [F, Input <: Tuple, Output](fn: F)(using ftc: Fn[F,Input, Output])
    def andThen(fn2: Output => Output): F = ftc.andThen(fn, fn2)
    @targetName("complexAndThenExt")
    def andThen[ZZ](fn2: Output => ZZ): FnCalc[Input, ZZ] = ftc.andThen(fn, fn2)

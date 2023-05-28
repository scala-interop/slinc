package fr.hammons.slinc.fnutils

import scala.quoted.*
import fr.hammons.slinc.modules.TransitionModule
import fr.hammons.slinc.DescriptorOf
import scala.util.chaining.*
import fr.hammons.slinc.Allocator

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

private def toNativeCompatibleImpl[A](
    a: Expr[A]
)(using Quotes, Type[A]): Expr[A] =
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

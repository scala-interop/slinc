package fr.hammons.slinc

import scala.quoted.*
import java.lang.invoke.MethodHandle
import fr.hammons.slinc.modules.TransitionModule
import scala.annotation.nowarn

object MethodHandleTools:
  def exprNameMapping(expr: Expr[Any])(using Quotes): String =
    if expr.isExprOf[Int] then "I"
    else if expr.isExprOf[Short] then "S"
    else if expr.isExprOf[Long] then "L"
    else if expr.isExprOf[Byte] then "B"
    else if expr.isExprOf[Double] then "D"
    else if expr.isExprOf[Float] then "F"
    else "O"

  def returnMapping[R](using Quotes, Type[R]) =
    import quotes.reflect.*

    TypeRepr.of[R].dealias.asType match
      case '[Int]    => "I"
      case '[Short]  => "S"
      case '[Long]   => "L"
      case '[Byte]   => "B"
      case '[Double] => "D"
      case '[Float]  => "F"
      case '[Unit]   => "O"
      case '[Object] => "O"
      case _         => "O"

  def invokeVariadicArguments(
      mhGen: Expr[Seq[TypeDescriptor] => MethodHandle],
      exprs: Expr[Seq[Any]],
      varArgDescriptors: Expr[Seq[TypeDescriptor]]
  )(using Quotes) =
    '{
      MethodHandleFacade.callVariadic(
        $mhGen($varArgDescriptors),
        $exprs*
      )
    }

  def invokeArguments[R](
      mh: Expr[MethodHandle],
      exprs: Seq[Expr[Any]]
  )(using
      Quotes,
      Type[R]
  ): Expr[Object | Null] =
    import quotes.reflect.*

    val arity = exprs.size

    val backupMod = TypeRepr
      .of[MethodHandleFacade]
      .classSymbol
      .getOrElse(report.errorAndAbort("This class should exist!!"))
      .companionModule

    val backupSymbol =
      backupMod.declaredMethods.find(_.name.endsWith(arity.toString()))

    backupSymbol
      .map(ms =>
        Apply(
          Select(Ident(backupMod.termRef), ms),
          mh.asTerm :: exprs.map(_.asTerm).toList
        ).asExprOf[Object | Null]
      )
      .getOrElse(
        '{ MethodHandleFacade.callVariadic($mh, ${ Varargs(exprs) }*) }
      )

  inline def getVariadicContext(s: Seq[Variadic]) =
    s.map(_.use[DescriptorOf](l ?=> _ => l.descriptor))

  def getVariadicExprs(s: Seq[Variadic])(using tm: TransitionModule) =
    (alloc: Allocator) ?=>
      s.map(
        _.use[DescriptorOf](dc ?=>
          d => tm.methodArgument(dc.descriptor, d, alloc)
        )
      )

  inline def wrappedMH[A](mem: Mem, methodHandle: MethodHandle) = ${
    wrappedMHImpl[A]('mem, 'methodHandle)
  }

  private def wrappedMHImpl[A](
      mem: Expr[Mem],
      methodHandleExpr: Expr[MethodHandle]
  )(using Quotes, Type[A]) =
    import quotes.reflect.*
    import scala.compiletime.asMatchable

    val (inputTypes, retType) = TypeRepr.of[A].asMatchable match
      case AppliedType(_, args) =>
        (args.init, args.last)
      case _ => report.errorAndAbort(TypeRepr.of[A].show)

    val paramNames = LazyList.iterate("a")(a => a ++ a)

    val expr = Lambda(
      Symbol.spliceOwner,
      MethodType(paramNames.take(inputTypes.size).toList)(
        _ => inputTypes,
        _ => retType
      ),
      (meth, params) =>
        retType.asType match
          case '[r] =>
            val invokeExpr = invokeArguments[r](
              methodHandleExpr,
              '{ $mem.asBase } +:
                params.map(_.asExpr)
            )
            val invokeResultExpr = '{
              val invokeResult = $invokeExpr
              if invokeResult == null then ().asInstanceOf[r]
              else invokeResult.asInstanceOf[r]
            }
            invokeResultExpr.asTerm
              .changeOwner(meth)
    ).asExprOf[A]

    expr

package fr.hammons.slinc

import scala.quoted.*
import scala.annotation.nowarn
import fr.hammons.slinc.CFunctionRuntimeInformation.{
  InputTransition,
  ReturnTransition
}
import fr.hammons.slinc.CFunctionBindingGenerator.VariadicTransition

trait CFunctionBindingGenerator:
  def generate(
      methodHandler: MethodHandler,
      transitionSet: CFunctionRuntimeInformation,
      variadicTransition: VariadicTransition,
      scope: Scope
  ): AnyRef

object CFunctionBindingGenerator:
  type VariadicTransition = (Allocator, Seq[Variadic]) => Seq[Any]

  private enum LambdaInputs:
    case Standard(args: List[Expr[Any]])
    case VariadicInputs(args: List[Expr[Any]], varArgs: Expr[Seq[Variadic]])

  private object LambdaInputs:
    def choose(args: List[Expr[Any]], isVariadic: Boolean)(
        variadicInput: => Expr[Seq[Variadic]]
    ) = if isVariadic then
      LambdaInputs.VariadicInputs(args, varArgs = variadicInput)
    else LambdaInputs.Standard(args)

  @nowarn("msg=unused implicit parameter")
  private def invokation(
      variadicTransition: Expr[VariadicTransition],
      mh: Expr[MethodHandler]
  )(using Quotes) =
    (alloc: Expr[Allocator], inputs: LambdaInputs) =>
      inputs match
        case LambdaInputs.Standard(args) =>
          MethodHandleTools.invokeArguments(
            '{ $mh.nonVariadic },
            args
          )
        case LambdaInputs.VariadicInputs(args, varArgs) =>
          '{
            MethodHandleFacade.callVariadic(
              $mh.variadic($varArgs),
              ${ Expr.ofList(args) } ++ $variadicTransition($alloc, $varArgs)*
            )
          }

  inline def apply[L](
      name: String
  ): CFunctionBindingGenerator = ${
    applyImpl[L]('name)
  }

  @nowarn("msg=unused implicit parameter")
  private def lambda(
      argNumbers: Int,
      scope: Expr[Scope],
      inputTransitions: Expr[IArray[InputTransition]],
      outputTransition: Expr[ReturnTransition],
      allocatingReturn: Boolean,
      varArg: Boolean
  )(
      invocationExpr: Quotes ?=> (
          Expr[Allocator],
          LambdaInputs
      ) => Expr[Object | Null]
  )(using Quotes) =
    import quotes.reflect.*

    val names = List.fill(argNumbers)("a")
    val argTypes =
      if varArg then
        List.fill(argNumbers - 1)(TypeRepr.of[Object]) :+ TypeRepr
          .of[Seq[Variadic]]
      else List.fill(argNumbers)(TypeRepr.of[Object])

    val methodType =
      MethodType(names)(_ => argTypes, _ => TypeRepr.of[Object])

    Lambda(
      Symbol.spliceOwner,
      methodType,
      (sym, inputs) =>
        def inputExprs(alloc: Expr[Allocator])(using q: Quotes) =
          val prefix = if allocatingReturn then List(alloc.asTerm) else Nil
          val toTransform =
            if varArg && inputs.nonEmpty then inputs.init
            else inputs
          LambdaInputs.choose(
            prefix
              .concat(toTransform)
              .map(_.asExpr)
              .zipWithIndex
              .map: (exp, i) =>
                '{ $inputTransitions(${ Expr(i) })($alloc, $exp) },
            varArg && inputs.nonEmpty
          )(inputs.last.asExprOf[Seq[Variadic]])

        '{
          $scope { alloc ?=>
            $outputTransition(
              ${ invocationExpr('alloc, inputExprs('alloc)) }
            )
          }
        }.asTerm.changeOwner(sym)
    ).asExprOf[AnyRef]

  @nowarn("msg=unused implicit parameter")
  private def applyImpl[L](name: Expr[String])(using
      Quotes,
      Type[L]
  ): Expr[CFunctionBindingGenerator] =
    import quotes.reflect.*

    val methodSymbol = TypeRepr
      .of[L]
      .classSymbol
      .get
      .declaredMethod(name.valueOrAbort)
      .head

    '{
      new CFunctionBindingGenerator:
        def generate(
            methodHandler: MethodHandler,
            functionInformation: CFunctionRuntimeInformation,
            variadicTransition: VariadicTransition,
            scope: Scope
        ): AnyRef =
          ${
            def lambdaGen(allocatingReturn: Boolean, variadic: Boolean) =
              lambda(
                methodSymbol.paramSymss.map(_.size).sum,
                'scope,
                '{ functionInformation.inputTransitions },
                '{ functionInformation.returnTransition },
                allocatingReturn,
                variadic
              )(invokation('variadicTransition, 'methodHandler))

            '{
              if functionInformation.isVariadic && functionInformation.returnAllocates
              then ${ lambdaGen(allocatingReturn = true, variadic = true) }
              else if functionInformation.isVariadic then
                ${ lambdaGen(allocatingReturn = false, variadic = true) }
              else if functionInformation.returnAllocates then
                ${ lambdaGen(allocatingReturn = true, variadic = false) }
              else ${ lambdaGen(allocatingReturn = false, variadic = false) }
            }
          }
    }

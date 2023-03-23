package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import scala.quoted.*
import scala.annotation.nowarn
import fr.hammons.slinc.modules.TransitionModule

type InputTransition = (Allocator, Any) => Any
type OutputTransition = (Object | Null) => AnyRef
trait CFunctionBindingGenerator:
  def generate(
      methodHandler: MethodHandler,
      inputTransitions: IArray[InputTransition],
      variadicTransition: TransitionModule,
      outputTransition: OutputTransition,
      scope: Scope,
      allocatingReturn: Boolean,
      variadic: Boolean
  ): AnyRef

object CFunctionBindingGenerator:
  private case class LambdaInputs(
      normalArgs: List[Expr[Any]],
      varArgs: Option[Expr[Seq[Variadic]]]
  )

  private def getVariadicExprs(
      s: Seq[Variadic],
      variadicTransition: TransitionModule,
      allocator: Allocator
  ): Seq[Any] =
    s.map: vararg =>
      vararg.use[DescriptorOf](dc ?=>
        d => variadicTransition.methodArgument(dc.descriptor, d, allocator)
      )

  private def invokation(
      vTransition: Expr[TransitionModule],
      mh: Expr[MethodHandler]
  )(using Quotes) =
    (alloc: Expr[Allocator], args: LambdaInputs) =>
      args.varArgs match
        case None =>
          MethodHandleTools.invokeArguments(
            '{ $mh.nonVariadic },
            args.normalArgs
          )
        case Some(varArgs) =>
          '{
            MethodHandleFacade.callVariadic(
              $mh.variadic($varArgs),
              ${ Expr.ofList(args.normalArgs) } ++ getVariadicExprs(
                $varArgs,
                $vTransition,
                $alloc
              )*
            )
          }

  inline def apply[L](
      name: String
  ): CFunctionBindingGenerator = ${
    applyImpl[L]('name)
  }

  private def lambda2(
      argNumbers: Int,
      scope: Expr[Scope],
      inputTransitions: Expr[IArray[InputTransition]],
      outputTransition: Expr[OutputTransition],
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
          val toTransform = if varArg then inputs.init else inputs
          LambdaInputs(
            prefix
              .concat(toTransform)
              .map(_.asExpr)
              .zipWithIndex
              .map: (exp, i) =>
                '{ $inputTransitions(${ Expr(i) })($alloc, $exp) },
            Option.unless(!varArg)(inputs.last.asExprOf[Seq[Variadic]])
          )

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
            inputTransitions: IArray[InputTransition],
            variadicTransition: TransitionModule,
            outputTransition: OutputTransition,
            scope: Scope,
            allocatingReturn: Boolean,
            variadic: Boolean
        ): AnyRef = ${
          val lamdaGen = (allocatingReturn: Boolean, variadic: Boolean) =>
            lambda2(
              methodSymbol.paramSymss.map(_.size).sum,
              'scope,
              'inputTransitions,
              'outputTransition,
              allocatingReturn,
              variadic
            )(invokation('variadicTransition, 'methodHandler))

          '{
            if allocatingReturn && variadic then ${ lamdaGen(true, true) }
            else if allocatingReturn then ${ lamdaGen(true, false) }
            else if variadic then ${ lamdaGen(false, true) }
            else ${ lamdaGen(false, false) }
          }
        }
    }

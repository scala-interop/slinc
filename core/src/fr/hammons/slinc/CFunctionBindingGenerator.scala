package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import scala.quoted.*
import scala.annotation.nowarn

type InputTransition = (Allocator, Any) => Any
type OutputTransition = (Object | Null) => AnyRef
type VariadicInputTransition = (TypeDescriptor, Allocator, Any) => Any
trait CFunctionBindingGenerator:
  def generate(
      methodHandler: MethodHandler,
      inputTransitions: IArray[InputTransition],
      outputTransition: OutputTransition,
      scope: Scope
  ): AnyRef

  def generateVariadic(
      methodHandler: MethodHandler,
      inputTransitions: IArray[InputTransition],
      variadicTransition: VariadicInputTransition,
      outputTransition: OutputTransition,
      scope: Scope
  ): AnyRef

object CFunctionBindingGenerator:
  private def getVariadicExprs(
      s: Seq[Variadic],
      variadicTransition: VariadicInputTransition,
      allocator: Allocator
  ): Seq[Any] =
    s.map: vararg =>
      vararg.use[DescriptorOf](dc ?=>
        d => variadicTransition(dc.descriptor, allocator, d)
      )

  @nowarn("msg=unused implicit parameter")
  private def invokeVariadicArguments(
      mhGen: Expr[Seq[Variadic] => MethodHandle],
      inputs: Expr[Seq[Any]],
      varArgs: Expr[Seq[Variadic]],
      variadicTransition: Expr[VariadicInputTransition],
      alloc: Expr[Allocator]
  )(using Quotes) =
    '{
      MethodHandleFacade.callVariadic(
        $mhGen($varArgs),
        $inputs ++ getVariadicExprs($varArgs, $variadicTransition, $alloc)*
      )
    }

  inline def apply[L](
      name: String
  ): CFunctionBindingGenerator = ${
    applyImpl[L]('name)
  }

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

    val argNumbers = methodSymbol.paramSymss.map(_.size).sum
    val names = List.fill(argNumbers)("a")

    def lambda(
        methodHandle: Expr[MethodHandle],
        inputTransitions: Expr[IArray[InputTransition]],
        outputTransition: Expr[OutputTransition],
        scope: Expr[Scope]
    )(using Quotes): Expr[AnyRef] =
      import quotes.reflect.*

      val argsTypes = List.fill(argNumbers)(TypeRepr.of[Object])

      val methodType =
        MethodType(names)(_ => argsTypes, _ => TypeRepr.of[Object])
      Lambda(
        Symbol.spliceOwner,
        methodType,
        (sym, inputs) =>
          def inputExprs(using q: Quotes) = (alloc: Expr[Allocator]) =>
            inputs
              .map(i => i.asExpr)
              .zipWithIndex
              .map((exp, i) =>
                '{ $inputTransitions(${ Expr(i) })($alloc, $exp) }
              )
          '{
            $scope { alloc ?=>
              $outputTransition(
                ${
                  MethodHandleTools.invokeArguments(
                    methodHandle,
                    inputExprs('alloc)
                  )
                }
              )
            }
          }.asTerm.changeOwner(sym)
      ).asExprOf[AnyRef]

    def lambdaVariadic(
        methodHandleGen: Expr[Seq[Variadic] => MethodHandle],
        inputTransitions: Expr[IArray[InputTransition]],
        outputTransition: Expr[OutputTransition],
        variadicTransition: Expr[
          VariadicInputTransition
        ],
        scope: Expr[Scope]
    )(using Quotes): Expr[AnyRef] =
      import quotes.reflect.*

      val argTypes = List.fill(argNumbers - 1)(TypeRepr.of[Object]) :+ TypeRepr
        .of[Seq[Variadic]]

      val methodType =
        MethodType(names)(_ => argTypes, _ => TypeRepr.of[Object])

      Lambda(
        Symbol.spliceOwner,
        methodType,
        (sym, inputs) =>
          def inputExprs(using q: Quotes) = (alloc: Expr[Allocator]) =>
            Expr.ofList(
              inputs.init
                .map(i => i.asExpr)
                .zipWithIndex
                .map((exp, i) =>
                  '{ $inputTransitions(${ Expr(i) })($alloc, $exp) }
                )
            )

          '{
            $scope { alloc ?=>
              $outputTransition(
                ${
                  invokeVariadicArguments(
                    methodHandleGen,
                    inputExprs('alloc),
                    inputs.last.asExprOf[Seq[Variadic]],
                    variadicTransition,
                    '{ alloc }
                  )
                }
              )
            }
          }.asTerm.changeOwner(sym)
      ).asExprOf[AnyRef]

    '{
      new CFunctionBindingGenerator:
        def generate(
            methodHandler: MethodHandler,
            inputTransitions: IArray[InputTransition],
            outputTransition: OutputTransition,
            scope: Scope
        ): AnyRef =
          ${
            lambda(
              '{ methodHandler.nonVariadic },
              'inputTransitions,
              'outputTransition,
              'scope
            )
          }

        def generateVariadic(
            methodHandler: MethodHandler,
            inputTransitions: IArray[InputTransition],
            variadicTransitions: VariadicInputTransition,
            outputTransition: OutputTransition,
            scope: Scope
        ): AnyRef = ${
          lambdaVariadic(
            '{ methodHandler.variadic },
            'inputTransitions,
            'outputTransition,
            'variadicTransitions,
            'scope
          )
        }
    }

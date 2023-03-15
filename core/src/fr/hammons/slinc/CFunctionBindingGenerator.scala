package fr.hammons.slinc

import java.lang.invoke.MethodHandle
import scala.quoted.*
import scala.annotation.nowarn

type InputTransition = (Allocator, Any) => Any
type OutputTransition = (Object | Null) => AnyRef
trait CFunctionBindingGenerator:
  def generate(
      methodHandle: MethodHandle,
      inputTransitions: IArray[InputTransition],
      outputTransition: OutputTransition,
      scope: Scope
  ): AnyRef

object CFunctionBindingGenerator:
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

    '{
      new CFunctionBindingGenerator:
        def generate(
            methodHandle: MethodHandle,
            inputTransitions: IArray[InputTransition],
            outputTransition: OutputTransition,
            scope: Scope
        ): AnyRef =
          ${
            lambda('methodHandle, 'inputTransitions, 'outputTransition, 'scope)
          }
    }

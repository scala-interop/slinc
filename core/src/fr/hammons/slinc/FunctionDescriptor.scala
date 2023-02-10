package fr.hammons.slinc

import scala.quoted.*
import fr.hammons.slinc.modules.DescriptorModule
import java.lang.invoke.MethodType
import scala.annotation.nowarn

final case class FunctionDescriptor(
    inputDescriptors: Seq[TypeDescriptor],
    variadicDescriptors: Seq[TypeDescriptor],
    outputDescriptor: Option[TypeDescriptor]
):
  def addVarargs(args: TypeDescriptor*) =
    FunctionDescriptor(inputDescriptors, args, outputDescriptor)

  def toMethodType(using DescriptorModule): MethodType =
    this match
      case FunctionDescriptor(head +: tail, variadicDescriptors, None) =>
        VoidHelper
          .methodTypeV(
            head.toCarrierType,
            tail.view.concat(variadicDescriptors).map(_.toCarrierType).toSeq*
          )
          .nn

      case FunctionDescriptor(
            head +: tail,
            variadicDescriptors,
            Some(outputDescriptor)
          ) =>
        MethodType
          .methodType(
            outputDescriptor.toCarrierType,
            head.toCarrierType,
            tail.view.concat(variadicDescriptors).map(_.toCarrierType).toSeq*
          )
          .nn

      case FunctionDescriptor(_, _, None) => VoidHelper.methodTypeV().nn
      case FunctionDescriptor(_, _, Some(outputDescriptor)) =>
        MethodType.methodType(outputDescriptor.toCarrierType).nn

object FunctionDescriptor:
  // grabs a description of a method from its definition. Ignores Seq[Variadic] arguments.
  // todo: get rid of this once bug https://github.com/lampepfl/dotty/issues/16863 is fixed
  @nowarn("msg=unused implicit parameter")
  @nowarn("msg=unused local definition")
  def fromDefDef(using q: Quotes)(symbol: q.reflect.Symbol) =
    import quotes.reflect.*
    val (inputRefs, outputType) = MacroHelpers.getInputsAndOutputType(symbol)

    val inputLayouts = Expr.ofSeq(
      inputRefs
        .filter {
          case '{ ${ _ }: Seq[Variadic] } => false
          case _                          => true
        }
        .map { case '{ ${ _ }: a } =>
          DescriptorOf.getDescriptorFor[a]
        }
    )

    val outputLayout = outputType match
      case '[Unit] => '{ None }
      case '[o] =>
        val descriptor = DescriptorOf.getDescriptorFor[o]
        '{ Some($descriptor) }

    '{
      FunctionDescriptor($inputLayouts, Seq.empty, $outputLayout)
    }

  inline def fromFunction[A] = ${
    fromFunctionImpl[A]
  }

  // todo: get rid of this once bug https://github.com/lampepfl/dotty/issues/16863 is fixed
  @nowarn("msg=unused implicit parameter")
  @nowarn("msg=unused local definition")
  private[slinc] def fromFunctionImpl[A](using Quotes, Type[A]) =
    val (inputTypes, outputType) = MacroHelpers.getInputTypesAndOutputTypes[A]

    val inputLayouts = Expr.ofSeq(inputTypes.map { case '[a] =>
      DescriptorOf.getDescriptorFor[a]
    })

    val outputLayout = outputType match
      case '[Unit] => '{ None }
      case '[o] =>
        val descriptor = DescriptorOf.getDescriptorFor[o]
        '{ Some($descriptor) }

    '{
      FunctionDescriptor($inputLayouts, Seq.empty, $outputLayout)
    }

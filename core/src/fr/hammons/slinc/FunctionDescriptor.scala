package fr.hammons.slinc

import scala.quoted.*
import fr.hammons.slinc.modules.DescriptorModule
import java.lang.invoke.MethodType

final case class FunctionDescriptor(
    inputDescriptors: Seq[TypeDescriptor],
    variadicDescriptors: Seq[TypeDescriptor],
    outputDescriptor: Option[TypeDescriptor]
):
  def toCFunctionDescriptor() = CFunctionDescriptor(
    Map.empty.withDefaultValue(""),
    inputDescriptors,
    variadicDescriptors.isEmpty,
    outputDescriptor
  )
  def addVarargs(args: TypeDescriptor*) =
    FunctionDescriptor(inputDescriptors, args, outputDescriptor)

  def toMethodType(using DescriptorModule): MethodType =
    this match
      case FunctionDescriptor(head +: tail, variadicDescriptors, None) =>
        VoidHelper
          .methodTypeV(
            head.toForeignTypeDescriptor.toCarrierType,
            tail.view
              .concat(variadicDescriptors)
              .map(_.toForeignTypeDescriptor.toCarrierType)
              .toSeq*
          )
          .nn

      case FunctionDescriptor(
            head +: tail,
            variadicDescriptors,
            Some(outputDescriptor)
          ) =>
        MethodType
          .methodType(
            outputDescriptor.toForeignTypeDescriptor.toCarrierType,
            head.toForeignTypeDescriptor.toCarrierType,
            tail.view
              .concat(variadicDescriptors)
              .map(_.toForeignTypeDescriptor.toCarrierType)
              .toSeq*
          )
          .nn

      case FunctionDescriptor(_, _, None) => VoidHelper.methodTypeV().nn
      case FunctionDescriptor(_, _, Some(outputDescriptor)) =>
        MethodType
          .methodType(outputDescriptor.toForeignTypeDescriptor.toCarrierType)
          .nn

object FunctionDescriptor:
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

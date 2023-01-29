package fr.hammons.slinc

import scala.quoted.*
import fr.hammons.slinc.modules.DescriptorModule
import java.lang.invoke.MethodType

final case class Descriptor(
    inputDescriptors: Seq[TypeDescriptor],
    variadicDescriptors: Seq[TypeDescriptor],
    outputDescriptor: Option[TypeDescriptor]
):
  def addVarargs(args: TypeDescriptor*) =
    Descriptor(inputDescriptors, args, outputDescriptor)

  def toMethodType(using DescriptorModule): MethodType =
    this match
      case Descriptor(head +: tail, variadicDescriptors, None) =>
        VoidHelper
          .methodTypeV(
            head.toCarrierType,
            tail.view.concat(variadicDescriptors).map(_.toCarrierType).toSeq*
          )
          .nn

      case Descriptor(
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

      case Descriptor(_, _, None) => VoidHelper.methodTypeV().nn
      case Descriptor(_, _, Some(outputDescriptor)) =>
        MethodType.methodType(outputDescriptor.toCarrierType).nn

object Descriptor:
  // grabs a description of a method from its definition. Ignores Seq[Variadic] arguments.
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
      Descriptor($inputLayouts, Seq.empty, $outputLayout)
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
      Descriptor($inputLayouts, Seq.empty, $outputLayout)
    }

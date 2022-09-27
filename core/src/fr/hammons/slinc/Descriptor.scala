package fr.hammons.slinc

import scala.quoted.*

final case class Descriptor(inputLayouts: Seq[DataLayout], outputLayout: Option[DataLayout])


object Descriptor:
  def fromDefDef(using q: Quotes)(symbol: q.reflect.Symbol) =
    val (inputRefs, outputType) = MacroHelpers.getInputsAndOutputType(symbol)

    val inputLayouts = Expr.ofSeq(inputRefs.map { case '{ ${ _ }: a } =>
      LayoutI.getLayoutFor[a]
    })
    val outputLayout = outputType match
      case '[Unit] => '{ None }
      case '[o] =>
        val layout = LayoutI.getLayoutFor[o]
        '{ Some($layout) }

    '{Descriptor($inputLayouts, $outputLayout)}

  inline def fromFunction[A] = ${
    fromFunctionImpl[A]
  }
  private[slinc] def fromFunctionImpl[A](using Quotes, Type[A]) = 
    val (inputTypes, outputType) = MacroHelpers.getInputTypesAndOutputTypes[A]

    val inputLayouts = Expr.ofSeq(inputTypes.map{ case '[a] => LayoutI.getLayoutFor[a]})
    
    val outputLayout = outputType match 
      case '[Unit] => '{ None }
      case '[o] => 
        val layout = LayoutI.getLayoutFor[o]
        '{Some($layout)}

    '{Descriptor($inputLayouts, $outputLayout)}
    

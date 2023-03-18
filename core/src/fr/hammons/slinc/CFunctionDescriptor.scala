package fr.hammons.slinc

import scala.annotation.nowarn
import scala.quoted.*

final case class CFunctionDescriptor(
    name: String,
    inputDescriptors: Seq[TypeDescriptor],
    isVariadic: Boolean,
    returnDescriptor: Option[TypeDescriptor]
)

object CFunctionDescriptor:
  inline def apply[L](name: String): CFunctionDescriptor = ${
    apply[L]('name)
  }

  @nowarn
  private def apply[L](
      name: Expr[String]
  )(using q: Quotes, t: Type[L]): Expr[CFunctionDescriptor] =
    import quotes.reflect.*

    val methodSymbol =
      TypeRepr.of[L].classSymbol.get.declaredMethod(name.valueOrAbort).head

    val (argumentTypes, returnType) =
      TypeRepr.of[L].memberType(methodSymbol) match
        case MethodType(_, args, ret) =>
          args -> ret
        case t =>
          report.errorAndAbort(
            s"C Function analog ${methodSymbol.fullName} has unsupported type ${t.show}"
          )

    val isVariadic = argumentTypes.last match
      case typ if typ =:= TypeRepr.of[Seq[Variadic]] =>
        true
      case _ => false

    val inputTypes = if isVariadic then argumentTypes.init else argumentTypes

    val inputDescriptors =
      Expr.ofList(inputTypes.map(TypeDescriptor.fromTypeRepr))
    val returnDescriptor = if returnType =:= TypeRepr.of[Unit] then '{ None }
    else
      val returnDescriptor = TypeDescriptor.fromTypeRepr(returnType)
      '{ Some($returnDescriptor) }

    '{
      new CFunctionDescriptor(
        ${ Expr(methodSymbol.name) },
        $inputDescriptors,
        ${ Expr(isVariadic) },
        $returnDescriptor
      )
    }

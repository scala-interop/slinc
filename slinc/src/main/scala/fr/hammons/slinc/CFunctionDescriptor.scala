package fr.hammons.slinc

import scala.annotation.nowarn
import scala.quoted.*
import types.{OS, Arch}
import fr.hammons.slinc.annotations.NameOverride

final case class CFunctionDescriptor(
    name: Map[(OS, Arch), String],
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

    val isVariadic = argumentTypes.lastOption match
      case Some(typ) if typ =:= TypeRepr.of[Seq[Variadic]] =>
        true
      case _ => false

    val inputTypes = if isVariadic then argumentTypes.init else argumentTypes

    val inputDescriptors =
      Expr.ofList(inputTypes.map(TypeDescriptor.fromTypeRepr))
    val returnDescriptor = if returnType =:= TypeRepr.of[Unit] then '{ None }
    else
      val returnDescriptor = TypeDescriptor.fromTypeRepr(returnType)
      '{ Some($returnDescriptor) }

    val nameOverrides = '{
      NameOverride[L](${ Expr(methodSymbol.name) })
        .flatMap: nameOverride =>
          nameOverride.platforms.map(tup => tup -> nameOverride.name)
        .toMap
    }

    '{
      new CFunctionDescriptor(
        $nameOverrides.withDefaultValue(${ Expr(methodSymbol.name) }),
        $inputDescriptors,
        ${ Expr(isVariadic) },
        $returnDescriptor
      )
    }

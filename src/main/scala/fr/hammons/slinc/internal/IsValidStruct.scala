package fr.hammons.slinc.internal

import fr.hammons.slinc.Struct
import quoted.*
import fr.hammons.slinc.Field
import cats.data.ValidatedNec
import cats.syntax.validated.*

sealed trait IsValidStruct[T <: Struct]

object IsValidStruct:
  inline implicit def proof[T <: Struct]: IsValidStruct[T] = ${
    proofImpl[T]
  }

  private def proofImpl[T <: Struct](using
      Quotes,
      Type[T]
  ): Expr[IsValidStruct[T]] =
    import quotes.reflect.*
    val structRepr = TypeRepr.of[Struct]
    val fieldSymbol = TypeRepr.of[Field].typeSymbol
    val tRepr = TypeRepr.of[T]
    def refinementTypeCheck(
        name: String,
        t: TypeRepr
    ): ValidatedNec[String, Unit] =
      if t.derivesFrom(fieldSymbol) then ().validNec
      else
        s"val $name: ${t.show} must have the field type `Field` or `>`".invalidNec

    def refinementCompatibilityCheck(
        name: String,
        t: TypeRepr
    ): ValidatedNec[String, Unit] =
      val proof = t.typeArgs.headOption.map(_.asType).flatMap { case '[a] =>
        Expr.summon[TypeDescription[a]]
      }

      if proof.isDefined then ().validNec
      else
        s"val $name: ${t.show} isn't a proper struct field due to the incompatible type".invalidNec

    def refinementSearch(t: TypeRepr): Either[String, Unit] =
      t match
        case Refinement(parent, name, typeRepr) =>
          ???

        case t if t =:= structRepr => Right(())
        case Refinement(_, name, typeRepr) =>
          Left(
            s"val $name: ${typeRepr.show} is not a valid member for a struct"
          )

    refinementSearch(TypeRepr.of[T]).fold(
      e =>
        report.errorAndAbort(
          s"${tRepr.show} is not a valid struct definition because $e"
        ),
      _ =>
        '{
          new IsValidStruct[T] {}
        }
    )

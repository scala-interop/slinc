package fr.hammons.slinc.internal

import fr.hammons.slinc.Struct
import quoted.*
import fr.hammons.slinc.Field
import cats.data.ValidatedNec
//import cats.syntax.validated.*
import cats.syntax.all.*

sealed trait IsValidStruct[T]

object IsValidStruct:
  inline implicit def proof[T]: IsValidStruct[T] = ${
    proofImpl[T]
  }

  private def proofImpl[T](using
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
        Expr.summon[StructCompatible[a]]
      }

      if proof.isDefined then ().validNec
      else
        s"val $name: ${t.show} isn't a proper struct field due to the incompatible type".invalidNec

    def refinementSearch(t: TypeRepr): ValidatedNec[String, Unit] =
      t match
        case Refinement(parent, name, typeRepr) =>
          refinementTypeCheck(name, typeRepr)
            .combine(refinementCompatibilityCheck(name, typeRepr))
            .combine(refinementSearch(parent))

        case t if t =:= structRepr =>
          if t =:= structRepr then ().validNec
          else
            s"Base of refinement ${tRepr.show} is not ${structRepr.show}".invalidNec

    refinementSearch(TypeRepr.of[T]).fold(
      e =>
        report.errorAndAbort(
          s"The struct definition supplied is not valid based on the following issues: ${e.toList.mkString("\n")}"
        ),
      _ =>
        '{
          new IsValidStruct[T] {}
        }
    )

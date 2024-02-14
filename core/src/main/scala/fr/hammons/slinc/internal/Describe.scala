package fr.hammons.slinc.internal

import fr.hammons.slinc.internal.ast.PTypeDescriptor
import quoted.*
import cats.syntax.all.*
import cats.data.ValidatedNec
import compiletime.summonInline
import fr.hammons.slinc.Struct
import fr.hammons.slinc.Transform

trait Describe[A] {
  def apply: PTypeDescriptor
}

object Describe:
  given [A: Type]: ToExpr[Describe[A]] with
    def apply(x: Describe[A])(using Quotes): Expr[Describe[A]] =
      '{
        new Describe[A]:
          def apply: PTypeDescriptor = ${ Expr(x.apply) }
      }

  inline given [A]: Describe[A] = ${
    implDef[A]
  }

  private def implDef[A](using q: Quotes, t: Type[A]): Expr[Describe[A]] = Expr(
    genFor[A]
  )

  def genFor[A](using Quotes, Type[A]): Describe[A] =
    Type.of[A] match
      case '[Byte] =>
        new Describe[A] {
          def apply: PTypeDescriptor = PTypeDescriptor.ByteTypeDescriptor
        }
      case '[Int] =>
        new Describe[A] {
          def apply: PTypeDescriptor = PTypeDescriptor.IntTypeDescriptor
        }
      case _ => genForUserDefined[A]

  def genForUserDefined[A](using Quotes, Type[A]): Describe[A] =
    val transformExists = Expr.summon[Transform[A, ?]]
    transformExists
      .map { case '{ ${ _ }: Transform[A, b] } =>
        new Describe[A]:
          def apply: PTypeDescriptor = genFor[b].apply
      }
      .getOrElse(???)

  // inline given [S](using IsValidStruct[S]): Describe[S] = ${
  //   structDescribeImpl[S]
  // }

  // private def structDescribeImpl[S](using Quotes, Type[S]): Expr[Describe[S]] =
  //   import quotes.reflect.*

  //   def traverseStruct(
  //       t: TypeRepr
  //   ): ValidatedNec[String, Seq[Expr[(String, PTypeDescriptor)]]] =
  //     t match
  //       case Refinement(parent, name, repr) =>
  //         repr.appliedTo(TypeRepr.of[Describe]).asType match
  //           case '[a & Describe[?]] =>
  //             Expr
  //               .summon[a & Describe[?]]
  //               .map(descOf => Seq('{ ${ Expr(name) } -> ${ descOf }.apply }))
  //               .toValidNec(s"Could not find ${Type.show[a]}")
  //               .combine(traverseStruct(parent))
  //       case t =>
  //         Seq.empty.validNec

  //   traverseStruct(TypeRepr.of[S])
  //     .map(exps => Expr.ofSeq(exps))
  //     .map(exp =>
  //       '{
  //         new Describe[S] {
  //           def apply: PTypeDescriptor =
  //             PTypeDescriptor.StructTypeDescriptor($exp*)
  //         }
  //       }
  //     )
  //     .fold(
  //       es =>
  //         report.errorAndAbort(s"Invalid struct: ${es.toList.mkString("\n")}"),
  //       identity
  //     )

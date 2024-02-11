package fr.hammons.slinc

import quoted.*
import deriving.Mirror
import compiletime.{summonInline, erasedValue, summonFrom, error}

opaque type Test2[A] = Int

object Test2:
  inline given Test2[Int] = 5
  inline given Test2[String] = 1

  inline def r[A](using inline i: Test2[A]): Int =
    i

trait TestTrait[A]:
  inline def x: Int

object TestTrait:
  given TestTrait[String] with
    inline val x = 5

  given TestTrait[Int] with
    inline val x = 1

  transparent inline def y[T <: Tuple]: Int =
    inline erasedValue[T] match
      case _: (h *: t) =>
        summonFrom {
          case t: TestTrait[h] => t.x + y[t]
          case _               => error("oops")
        }
      case _: EmptyTuple => 0

  inline given product[P <: Product](using m: Mirror.ProductOf[P]): TestTrait[P]
  with {
    inline def x: Int = productCalc[P]

  }

  private transparent inline def productCalc[P] = ${
    productImpl[P]
  }
  private def productImpl[P](using Quotes, Type[P]): Expr[Int] =
    import quotes.reflect.*

    val pRepr = TypeRepr.of[P]
    val caseFields = pRepr.classSymbol.map(_.caseFields).getOrElse(???)
    val values = caseFields
      .map(pRepr.memberType)
      .map(_.asType)
      .map { case '[a] =>
        val tt = Expr.summon[TestTrait[a]].getOrElse(???)
        '{
          ???
        }
      }
      .map(exp =>
        Select(
          exp.asTerm,
          TypeRepr.of[TestTrait[?]].classSymbol.get.declaredField("x")
        )
      )

    report.errorAndAbort(
      s"fields: ${TypeRepr.of[TestTrait[?]].classSymbol.get.children} methods: ${TypeRepr
          .of[TestTrait[?]]}"
    )

    // val value = values.sum

    // val refinement = Refinement(
    //   TypeRepr.of[TestTrait[P]],
    //   "x",
    //   Singleton.apply(Expr(value).asTerm).tpe
    // ).asType

    // Expr(value)

  transparent inline def r[A](using inline i: TestTrait[A]): Int =
    i.x

package fr.hammons.slinc.fset

import scala.quoted.*
import java.util.concurrent.atomic.AtomicReference

class FSetBacking[A](arr: IArray[AtomicReference[AnyRef]]) extends Selectable:
  transparent inline def applyDynamic(
      inline name: String,
      inline `types`: Class[?]*
  )(
      inline args: Any*
  ) = ${
    FSetBacking.applyDynamicImpl[A]('name, 'args, 'arr)
  }

object FSetBacking:
  def applyDynamicImpl[A](
      name: Expr[String],
      args: Expr[Seq[Any]],
      fns: Expr[IArray[AtomicReference[AnyRef]]]
  )(using Quotes, Type[A]): Expr[Any] =
    import quotes.reflect.*

    import scala.compiletime.asMatchable

    val methodSymbol =
      TypeRepr.of[A].classSymbol.get.declaredMethod(name.valueOrAbort).head
    val methodPositionExpr =
      Expr(TypeRepr.of[A].classSymbol.get.declaredMethods.indexOf(methodSymbol))

    val (fnType, rt) = TypeRepr.of[A].memberType(methodSymbol).asMatchable match
      case MethodType(_, types, rt) =>
        Symbol
          .classSymbol(s"scala.Function${types.size}")
          .typeRef
          .appliedTo(types :+ rt) -> rt

    val fnTerm =
      fnType.asType match
        case '[a] =>
          import scala.language.unsafeNulls
          '{ $fns($methodPositionExpr).get().asInstanceOf[a] }.asTerm

    val inputs = args match
      case Varargs(inputs) => inputs
    val code =
      ValDef.let(Symbol.spliceOwner, inputs.map(_.asTerm).toList): refs =>
        Apply(
          Select(fnTerm, fnType.classSymbol.get.declaredMethod("apply").head),
          refs
        )

    rt.asType match
      case '[r] =>
        code.asExprOf[r]
